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

(ns repopreview.github.middleware
  (:require
   [clojure.string :as str]
   [hiccup.util :refer [escape-html]]
   [repopreview.github :as github]
   [repopreview.template :as template]
   [taoensso.timbre :as timbre]))

(defn wrap-supply-repo
  "Middleware to extract the repository from the request"
  [handler]
  (fn [{{:keys [username repo-name]} :route-params :as req}]
    (let [repo-str (str username "/" repo-name)
          repo     (github/repository username repo-name)]
      (timbre/debug {:message "Fetching repository" :repo repo-str})
      (-> req
          (assoc :repopreview/repo repo
                 :repopreview/repo-name repo-str)
          (handler)))))

(defn wrap-supply-branch-and-path
  "Middleware to extract the branch name and selected path from the request."
  [handler]
  (fn [{repo :repopreview/repo {:keys [path]} :route-params :as req}]
    (let [[branch-name tree-path] (->> (github/branches repo)
                                       (github/branch-name-and-path path))]
      (timbre/debug {:message "Using branch and path"
                     :branch  branch-name
                     :path    tree-path})
      (-> req
          (assoc :repopreview/branch-name branch-name
                 :repopreview/path tree-path)
          (handler)))))

(defn wrap-supply-ref
  "Middleware to determine the Git ref being requested."
  [handler]
  (fn [{:keys [repopreview/repo repopreview/branch-name] :as req}]
    (let [ref (if (some? branch-name)
                (github/branch-ref repo branch-name)
                (github/branch-ref repo))]
      (timbre/debug {:message "Using ref" :ref ref})
      (-> req
          (assoc :repopreview/ref ref)
          (handler)))))

(defn repo-xform
  "Transducer to apply query filters to the final result set for rendering to
  the client."
  [{:keys [max-hits exclude-pattern include-pattern]}]
  (let [fns (cond-> []
              (string? include-pattern)
              (conj (filter #(str/includes? (:name %) include-pattern)))

              (string? exclude-pattern)
              (conj (remove #(str/includes? (:name %) exclude-pattern)))

              true
              (conj (map #(update % :content escape-html)))

              true
              (conj (map template/code-file)))]
    (apply comp fns)))

(defn repo-file-comp
  [{f1-name :name f1-path :path} {f2-name :name f2-path :path}]
  (cond
    (str/starts-with? (str/lower-case f1-path) "readme") -1
    (str/starts-with? (str/lower-case f2-path) "readme") 1
    :else                                             (compare f1-path f2-path)))

(defn wrap-supply-sort-and-limit
  "Middleware which supplies a sorting and limiting function that can help
  reduce the number of files whose contents are fetched from GitHub."
  [handler]
  (fn [{{:keys [max-hits]} :query-params :as req}]
    (let [max-hits       (try
                           (let [parsed (Integer/parseInt max-hits)]
                             (if (and (pos-int? parsed) (< 0 parsed 100))
                               parsed
                               20))
                           (catch NumberFormatException _ 20))
          sort-and-limit (fn [tree]
                           (timbre/debug {:message  "Limiting fetched files"
                                          :max-hits max-hits})
                           (->> tree
                                (sort repo-file-comp)
                                (take max-hits)))]
      (-> req
          (assoc :repopreview/sort-and-limit sort-and-limit)
          (handler)))))

(defn wrap-supply-xform
  "Middleware to supply a transducer that can be used to reduce the collection
  of files formatted and returned to the client."
  [handler]
  (fn [{:keys [query-params] :as req}]
    (let [xform (repo-xform query-params)]
      (-> req
          (assoc :repopreview/xform xform)
          (handler)))))
