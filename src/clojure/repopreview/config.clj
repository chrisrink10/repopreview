;; Copyright 2016 Chris Rink
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

(ns repopreview.config
  (:require
   [clojure.spec.alpha :as s]
   [cprop.core :as cprop]
   [cprop.source :as source]))

(s/def ::github (s/keys :req-un [:repopreview.github/api-key]))

(s/def ::logging (s/keys :req-un [:repopreview.logging/level]))

(s/def ::web-server (s/keys :req-un [:repopreview.web-server/port]
                            :opt-un [:repopreview.web-server/ip
                                     :repopreview.web-server/thread
                                     :repopreview.web-server/queue-size
                                     :repopreview.web-server/max-body
                                     :repopreview.web-server/max-line]))

(s/def ::repopreview (s/keys :req-un [::github
                                      ::logging
                                      ::web-server]))

(s/def ::config (s/keys :req-un [::repopreview]))

(defn read-env-config
  "Read the application configuration from the application defaults,
  environment variables, and any specified overrides.

  This function rebinds `*out*' to suppress `println' output from the
  cprop library.

  Returns the configuration formed by merging (1) the defaults, (2)
  environment variables, and (3) any overrides specified as an argument."
  [overrides]
  (binding [*out* (new java.io.StringWriter)]
    (cprop/load-config :merge [(source/from-env) overrides])))

(defn read-config
  "Read the application configuration using `read-env-config' and
  validate it against the config spec.

  Throws an exception if the spec fails. Returns the configuration
  otherwise."
  ([]
   (read-config {}))
  ([overrides]
   (let [config (read-env-config overrides)]
     (if (s/valid? ::config config)
       (:repopreview config)
       (throw
        (ex-info (s/explain-str ::config config)
                 (s/explain-data ::config config)))))))

(def config-cache
  (memoize read-config))

(defn config
  "Read configuration values from the cached environment config.

  If arguments are provided, they are treated as in `get-in' to
  access sub-sections of the configuration map."
  ([]
   (config-cache))
  ([ks]
   (-> (config-cache) (get-in ks)))
  ([ks not-found]
   (-> (config-cache) (get-in ks not-found))))
