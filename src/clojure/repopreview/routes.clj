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
   [clojure.walk :as walk]
   [bidi.ring :as bidi]
   [hiccup.core :refer [html]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]
   [repopreview.github :as github]
   [repopreview.github.middleware :as github.middleware]
   [repopreview.middleware :refer [wrap-apply-template
                                   wrap-catch-exceptions
                                   wrap-keywordize-query-params
                                   wrap-supply-highlight-js]]
   [repopreview.template :as template]))

(defn index-handler
  [_]
  {:title   "Repo Preview"
   :header  (template/header "Repo Preview")
   :content [:p "Preview a GitHub repository on a single page!"]})

(defn repo-root-handler
  [{:keys [repopreview/repo
           repopreview/repo-name
           repopreview/ref
           repopreview/sort-and-limit
           repopreview/xform] :as req}]
  {:title   (str repo-name " - Repo Preview")
   :header  (template/header "Repo Preview" repo-name)
   :content (->> (github/repo-tree repo ref)
                 (sort-and-limit)
                 (github/fetch-contents repo ref)
                 (into [:div] xform))})

(defn repo-branch-path
  [{:keys [repopreview/repo
           repopreview/repo-name
           repopreview/path
           repopreview/ref
           repopreview/sort-and-limit
           repopreview/xform] :as req}]
  {:title   (str repo-name " - Repo Preview")
   :header  (template/header "Repo Preview" repo-name)
   :content (->> (github/repo-path repo path)
                 (sort-and-limit)
                 (github/fetch-contents repo ref)
                 (into [:div] xform))})

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

(defn internal-error-handler
  [_]
  (-> {:title  "Internal Server Error - Repo Preview"
       :header  (template/header "Repo Preview" "Internal Server Error" :danger)
       :content [:p "Awww, man! Something went wrong on our end. We're working on fixing it though!"]}
      (template/page)
      (html)
      (response/response)
      (response/status 500)
      (response/header "Content-Type" "text/html")))

(def route-map
  ["/" [["" {:get :index}]

        [[:username "/" :repo-name] {:get :repo-root}]

        [[:username "/" :repo-name "/tree/" [ #".*" :path ]]
         {:get :repo-branch-path}]

        [[:username "/" :repo-name "/blob/" [ #".*" :path ]]
         {:get :repo-branch-path}]

        [true           :not-found]]])

(def handler-map
  {:index (-> index-handler
              (wrap-apply-template template/page))

   :repo-root        (-> repo-root-handler
                         (github.middleware/wrap-supply-xform)
                         (github.middleware/wrap-supply-sort-and-limit)
                         (github.middleware/wrap-supply-ref)
                         (github.middleware/wrap-supply-repo)
                         (wrap-supply-highlight-js)
                         (wrap-apply-template template/page))
   :repo-branch-path (-> repo-branch-path
                         (github.middleware/wrap-supply-xform)
                         (github.middleware/wrap-supply-sort-and-limit)
                         (github.middleware/wrap-supply-ref)
                         (github.middleware/wrap-supply-branch-and-path)
                         (github.middleware/wrap-supply-repo)
                         (wrap-supply-highlight-js)
                         (wrap-apply-template template/page))

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
      (wrap-catch-exceptions internal-error-handler)))
