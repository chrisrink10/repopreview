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

(ns repopreview.web-server
  (:require
   [clojure.spec.alpha :as s]
   [mount.core :refer [defstate]]
   [org.httpkit.server :as http]
   [taoensso.timbre :as timbre]
   [repopreview.config :as config]
   [repopreview.routes :as routes]))

(s/def ::ip string?)
(s/def ::port (s/int-in 1 65536))
(s/def ::thread (s/int-in 1 9))
(s/def ::queue-size (s/int-in 10000 100000))
(s/def ::max-body (s/int-in 524288 16777216))
(s/def ::max-line (s/int-in 512 8192))

(s/def ::opts (s/keys :req-un [::port]
                      :opt-un [::ip ::thread ::queue-size ::max-body ::max-line]))

(s/def ::stop-fn ifn?)

(s/fdef start
        :args (s/cat :opts (s/? ::opts)))
(defn start
  "Start the HTTP Kit server."
  ([]
   (-> [:web-server] (config/config) (start)))
  ([opts]
   (try
     (let [server (http/run-server #'routes/repopreview-app opts)]
       (timbre/info {:message "Web server started" :opts opts})
       server)
     (catch Throwable t
       (timbre/error {:message   "Failed to start web server"
                      :opts      opts
                      :exception t})
       (throw t)))))

(s/fdef stop
        :args (s/cat :server ::stop-fn))
(defn stop
  "Stop the HTTP Kit server.

  HTTP Kit's `(run-server)' function returns a function that can
  be used to stop the server. This function accepts that function
  and calls it."
  [server]
  (try
    (do
      (server)
      (timbre/info {:message "Web server stopped"}))
    (catch Throwable t
      (timbre/error {:message   "Failed to stop web server"
                     :exception t})
      (throw t))))

(defstate web-server
  :start (start)
  :stop  (stop web-server))
