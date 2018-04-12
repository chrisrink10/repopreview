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

(ns repopreview.logging
  (:require
   [clojure.spec.alpha :as s]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [repopreview.config :as config]))

(s/def ::level #{:debug :info :warn :error :fatal})

(defn level
  "Read the logging level configuration from the environment."
  []
  (config/config [:logging :level]))

(defstate timbre-config
  :start (timbre/set-level! (level)))
