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

(ns repopreview.template)

(defn code-file
  [{:keys [name sha content]}]
  [:div
   [:h4 {:id (str "file-" sha) :class "title is-4"} name]
   [:pre [:code content]]])

(defn page
  [{:keys [title header content links scripts]}]
  `[:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title ~title]
     [:link {:rel         "stylesheet"
             :type        "text/css"
             :href        "//cdnjs.cloudflare.com/ajax/libs/bulma/0.6.2/css/bulma.min.css"
             :integrity   "sha256-2k1KVsNPRXxZOsXQ8aqcZ9GOOwmJTMoOB5o5Qp1d6/s="
             :crossorigin "anonymous"}]
     ~@links]
    [:body
     ~header
     [:section {:class "section"}
      ~content]
     [:footer {:class "footer"}
      [:div {:class "container"}
       [:div {:class "content has-text-centered"}
        [:p
         [:strong "Repo Summary"]
         " by "
         [:a {:href "https://crink.io"} "Chris Rink"]
         ". "
         "The source code is licensed "
         [:a {:ref "http://opensource.org/licenses/mit-license.php"} "MIT"]
         "."]]]]
     [:script {:defer true :src "//use.fontawesome.com/releases/v5.0.6/js/all.js"}]
     ~@scripts]])

(defn header
  ([title]
   (header title nil))
  ([title subtitle]
   (header title subtitle :dark))
  ([title subtitle style]
   (let [style-class (case style
                       :warning "is-warning"
                       :danger  "is-danger"
                       "is-dark")]
     [:section {:class (str "hero " style-class)}
      [:div {:class "hero-body"}
       [:div {:class "container"}
        [:h1 {:class "title"}
         title]
        (when (some? subtitle)
          [:h2 {:class "subtitle"}
           subtitle])]]])))
