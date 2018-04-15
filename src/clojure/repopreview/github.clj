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
   org.kohsuke.github.GHTreeEntry
   org.kohsuke.github.HttpException)
  (:require
   [clojure.core.async :as async]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [repopreview.config :as config]))

(s/def ::api-key string?)

(defstate ^GitHub github
  :start (-> (config/config [:github :api-key])
             (GitHub/connectUsingOAuth)))

(defn repository
  "Get a GitHub repository handle."
  [username repo-name]
  (->> (str username "/" repo-name)
       (.getRepository github)))

(defn branches
  "Get all remote branches from the repository."
  [^GHRepository repo]
  (.getBranches repo))

(defn branch-name-and-path
  "Split a requested URI path into the name of a branch and the remaining
  path requested (if any)."
  [path branch-names]
  (let [matches (->> (keys branch-names)
                     (filter #(str/starts-with? path %)))]
    (if (not= 1 (count matches))
      (throw
       (ex-info "Could not identify unique branch name from path"
                {:matches matches
                 :path    path}))
      (let [branch-name (first matches)
            tree-path   (str/replace-first path branch-name "")]
        [branch-name
         (cond-> tree-path
           (str/starts-with? tree-path "/") (subs 1))]))))

(defn branch-ref
  "Get a ref (as a SHA1) for the most recent commit on the named branch.
  If no branch is specified, use the default branch for the repository."
  ([^GHRepository repo]
   (->> (.getDefaultBranch repo)
        (branch-ref repo)))
  ([^GHRepository repo branch-name]
   (let [^GHBranch branch (->> branch-name
                               (.getBranch repo))]
     (.getSHA1 branch))))

(defn file-contents
  "Get the file contents of a file object from GitHub as map."
  [^GHRepository repo ref {:keys [path] :as f}]
  (let [^GHContent contents (.getFileContent repo path ref)]
    (assoc f
           :name (.getName contents)
           :content (->> (.read contents) (slurp)))))

(defn fetch-contents
  "Fetch the file contents of an entire Git tree object into memory."
  [^GHRepository repo ref tree]
  (let [in-ch  (async/to-chan tree)
        out-ch (async/chan 1000)]
    (as-> (map #(file-contents repo ref %)) $
      (async/pipeline-blocking 8 out-ch $ in-ch true))
      (async/<!! (async/into [] out-ch))))

(defn repo-tree
  "Get a Git tree for a ref. If no ref is specified, get the tree for
  the default branch of the repository."
  ([^GHRepository repo]
   (->> (branch-ref repo)
        (repo-tree repo)))
  ([^GHRepository repo ref]
   (let [^GHTree tree (.getTreeRecursive repo ref 1)
         xform        (comp (filter #(= (.getType ^GHTreeEntry %) "blob"))
                            (map (fn [^GHTreeEntry entry]
                                   {:path (.getPath entry)
                                    :sha  (.getSha entry)
                                    :mode (.getMode entry)
                                    :size (.getSize entry)})))]
     (->> (.getTree tree)
          (sequence xform)))))

(declare repo-path-iter)

(defn- repo-path-iter
  "Get a flattened, recursive listing of all sub-tree files and directories of
  the requested file."
  [^GHRepository repo ^GHContent file]
  (cond
    (.isDirectory file)
    (->> (.listDirectoryContent file)
         (mapcat #(repo-path-iter repo %)))

    (.isFile file)
    [{:name (.getName file)
      :path (.getPath file)
      :sha  (.getSha file)
      :size (.getSize file)}]

    :else
    nil))

(defn repo-path
  "Get a sub-tree of a repository path starting at path and recursively requesting
  all sub-trees."
  [^GHRepository repo path]
  (try
    (let [^GHContent file (.getFileContent repo path)]
      (repo-path-iter repo file))
    (catch HttpException e
      (let [^java.util.List dir (.getDirectoryContent repo path)]
        (mapcat #(repo-path-iter repo %) dir)))))
