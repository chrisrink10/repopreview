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

(ns repopreview.middleware
  (:require
   [clojure.walk :as walk]
   [hiccup.core :refer [html]]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]))

(defn wrap-apply-template
  [handler template]
  (fn [req]
    (-> (handler req)
        (template)
        (html)
        (response/response)
        (response/status 200)
        (response/header "Content-Type" "text/html"))))

(defn wrap-catch-exceptions
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (do(timbre/error {:message   "Exception occurred processing request"
                          :exception e})
           (-> (response/response "Error")
               (response/status 500)))))))

(defn wrap-keywordize-query-params
  [handler]
  (fn [{:keys [query-params] :as req}]
    (->> (walk/keywordize-keys query-params)
         (assoc req :query-params)
         (handler))))
