;; Copyright 2018 Chris Rink
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns repopreview.github
  (:import
   org.kohsuke.github.GitHub
   org.kohsuke.github.GHBranch
   org.kohsuke.github.GHContent
   org.kohsuke.github.GHRepository
   org.kohsuke.github.GHTree
   org.kohsuke.github.GHTreeEntry)
  (:require
   [clojure.core.async :as async]
   [clojure.spec.alpha :as s]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [repopreview.config :as config]))

(s/def ::api-key string?)

(defstate ^GitHub github
  :start (-> (config/config [:github :api-key])
             (GitHub/connectUsingOAuth)))

(defn repository
  [username repo-name]
  (->> (str username "/" repo-name)
       (.getRepository github)))

(defn branch-ref
  ([^GHRepository repo]
   (->> (.getDefaultBranch repo)
        (branch-ref repo)))
  ([^GHRepository repo branch-name]
   (let [^GHBranch branch (->> branch-name
                               (.getBranch repo))]
     (.getSHA1 branch))))

(defn file-contents
  [^GHRepository repo ref path]
  (let [^GHContent contents (.getFileContent repo path ref)]
    {:name    (.getName contents)
     :path    (.getPath contents)
     :sha     (.getSha contents)
     :content (->> (.read contents) (slurp))}))

(defn fetch-contents
  [^GHRepository repo ref tree]
  (let [in-ch (async/to-chan tree)
          out-ch  (async/chan 1000)]
    (as-> (map #(file-contents repo ref %)) $
      (async/pipeline-blocking 8 out-ch $ in-ch true))
      (async/<!! (async/into [] out-ch))))

(defn repo-tree
  ([^GHRepository repo]
   (->> (branch-ref repo)
        (repo-tree repo)))
  ([^GHRepository repo ref]
   (let [^GHTree tree (.getTreeRecursive repo ref 1)]
     (->> (.getTree tree)
          (filter #(= (.getType ^GHTreeEntry %) "blob"))
          (map #(.getPath ^GHTreeEntry %))))))

