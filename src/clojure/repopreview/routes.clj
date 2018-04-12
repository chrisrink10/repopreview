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

(ns repopreview.routes
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [bidi.ring :as bidi]
   [hiccup.core :refer [html]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]
   [repopreview.github :as github]
   [repopreview.middleware :refer [wrap-apply-template
                                   wrap-catch-exceptions
                                   wrap-keywordize-query-params]]
   [repopreview.template :as template]))

(defn index-handler
  [_]
  {:title   "Repo Preview"
   :header  (template/header "Repo Preview")
   :content [:p "Preview a GitHub repository on a single page!"]})

(defn repo-file-comp
  [{f1-name :name f1-path :path} {f2-name :name f2-path :path}]
  (cond
    (str/starts-with? (str/lower-case f1-name) "readme") -1
    (str/starts-with? (str/lower-case f2-name) "readme") 1
    :else                                             (compare f1-path f2-path)))

(defn repo-xform
  [{:keys [max-hits exclude-pattern include-pattern]} & others]
  (let [max-hits (if (pos-int? max-hits) max-hits 20)

        fns (cond-> [(take max-hits)]
              (string? include-pattern) (conj (filter #(str/includes? (:name %) include-pattern)))
              (string? exclude-pattern) (conj (remove #(str/includes? (:name %) exclude-pattern)))
              (some? others)            (into others))]
    (apply comp fns)))

(defn repo-root-handler
  [{{:keys [username repo-name]} :route-params query-params :query-params :as req}]
  (let [repo-str (str username "/" repo-name)
        repo     (github/repository username repo-name)
        ref      (github/branch-ref repo)
        xform    (->> (map template/code-file)
                      (repo-xform query-params))
        tree     (->> (github/repo-tree repo ref)
                      (github/fetch-contents repo ref)
                      (sort repo-file-comp)
                      (into [:div] xform))]
    {:title   (str repo-str " - Repo Preview")
     :header  (template/header "Repo Preview" repo-str)
     :content tree
     :links   [[:link {:rel  "stylesheet"
                       :href "//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.12.0/styles/default.min.css"}]]
     :scripts [[:script {:src "//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.12.0/highlight.min.js"}]
               [:script "hljs.initHighlightingOnLoad();"]]}))

(defn not-found-handler
  [_]
  (-> {:title  "Page Not Found - Repo Preview"
       :header  (template/header "Repo Preview" "Page Not Found" :warning)
       :content [:p "Uh oh! We couldn't find the repository you requested."]}
      (template/page)
      (html)
      (response/response)
      (response/status 404)
      (response/header "Content-Type" "text/html")))

(def route-map
  ["/" [["" {:get :index}]

        [[:username "/" :repo-name] {:get :repo-root}]

        [[:username "/" :repo-name "/blob/" :branch-name]
         {:get :repo-root-branch}]

        [[:username "/" :repo-name "/blob/" :branch-name "/" [ #".*" :path ]]
         {:get :repo-branch-path}]
        [true           :not-found]]])

(def handler-map
  {:index (-> index-handler
              (wrap-apply-template template/page))

   :repo-root        (-> repo-root-handler
                         (wrap-apply-template template/page))
   :repo-root-branch nil
   :repo-branch-path nil

   :js-resource (bidi/resources {:prefix "public/js"})
   :not-found   not-found-handler})

(def repopreview-app
  "Create the Repo Preview server app that can be used by
  HTTP Kit."
  (-> handler-map
      (walk/postwalk-replace route-map)
      (bidi/make-handler)
      (wrap-keywordize-query-params)
      (wrap-params)
      (wrap-catch-exceptions)))
