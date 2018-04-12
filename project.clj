(defproject repopreview "0.1.0-SNAPSHOT"
  :description "Preview GitHub repositories in a single page."
  :license {:name "Apache License 2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"
                  :exclusions [org.clojure/tools.reader]]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.reader "1.2.1"]

                 [mount "0.1.12"]
                 [medley "0.8.4"]
                 [cprop "0.1.11"]
                 [com.taoensso/timbre "4.10.0"
                  :exclusions [org.clojure/tools.reader]]

                 [http-kit "2.2.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-devel "1.6.3"]
                 [bidi "2.1.2"]
                 [cheshire "5.7.1"]
                 [hiccup "1.0.5"]
                 [org.kohsuke/github-api "1.92"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]]]

  :source-paths ["src/clojure"]
  :resource-paths ["resources"]

  :profiles {:uberjar {:aot            :all
                       :resource-paths ["env/prod/resources"]}
             :dev     {:dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                        [org.clojure/tools.nrepl "0.2.13"]
                                        [org.clojure/tools.trace "0.7.9"]]
                       :source-paths   ["env/dev/src"]
                       :resource-paths ["env/dev/resources"]
                       :repl-options   {:init-ns dev
                                        :init    (set! *print-length* 50)}}}

  :target-path "target/%s/"

  :main repopreview.core)
