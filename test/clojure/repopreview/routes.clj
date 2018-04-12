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
   [bidi.bidi :refer [match-route]]
   [clojure.test :refer [deftest is testing]]
   [repopreview.routes :as routes]))

(deftest routes-match
  (testing "index"
    (let [{:keys [handler request-method]}
          (match-route routes/route-map "/" :request-method :get)]
      (is (= handler :index))
      (is (= request-method :get))))

  (testing "repo-root"
    (let [{:keys [handler request-method route-params]}
          (match-route routes/route-map "/chrisrink10/repopreview" :request-method :get)]
      (is (= handler :repo-root))
      (is (= request-method :get))
      (is (= "chrisrink10" (:username route-params)))
      (is (= "repopreview" (:repo-name route-params)))))

  (testing "repo-root-branch"
    (let [{:keys [handler request-method route-params]}
          (match-route routes/route-map "/chrisrink10/repopreview/blob/master" :request-method :get)]
      (is (= handler :repo-root-branch))
      (is (= request-method :get))
      (is (= "chrisrink10" (:username route-params)))
      (is (= "repopreview" (:repo-name route-params)))
      (is (= "master" (:branch-name route-params)))))

  (testing "repo-branch-path"
    (let [{:keys [handler request-method route-params]}
          (match-route routes/route-map
                       "/chrisrink10/repopreview/blob/master/README.md"
                       :request-method :get)]
      (is (= handler :repo-branch-path))
      (is (= request-method :get))
      (is (= "chrisrink10" (:username route-params)))
      (is (= "repopreview" (:repo-name route-params)))
      (is (= "master" (:branch-name route-params)))
      (is (= "README.md" (:path route-params))))

    (let [{:keys [handler request-method route-params]}
          (match-route routes/route-map
                       "/chrisrink10/repopreview/blob/master/some/deeper/path/file.clj"
                       :request-method :get)]
      (is (= handler :repo-branch-path))
      (is (= request-method :get))
      (is (= "chrisrink10" (:username route-params)))
      (is (= "repopreview" (:repo-name route-params)))
      (is (= "master" (:branch-name route-params)))
      (is (= "some/deeper/path/file.clj" (:path route-params)))))

  (testing "not-found"
    (let [{:keys [handler]}
          (match-route routes/route-map "/username")]
      (is (= handler :not-found)))

    (let [{:keys [handler request-method route-params]}
          (match-route routes/route-map "/chrisrink10/repopreview" :request-method :post)]
      (is (= handler :not-found)))

    (let [{:keys [handler request-method route-params]}
          (match-route routes/route-map "/chrisrink10/repopreview/blob/bloop" :request-method :post)]
      (is (= handler :not-found)))))
