(ns teglon.db-test
  (:require [teglon.db :as db])
  (:require [teglon.repo :as repo])
  (:use [clojure.test]))

;; using a snapshot of clojars.org for testing

(deftest test-repo-indexing
  (repo/init-repo "/Users/liebke/Desktop/clojars/clojars.org/repo")
  (is (= 966 (count (keys @db/*cljr-repo-db*))))
  (is (= 347 (count (keys @db/*cljr-index-by-group*))))
  (is (= 485 (count (keys @db/*cljr-index-by-group-name*))))
  (is (= 985 (count (keys @db/*cljr-index-by-text*)))))

(deftest test-get-model
  (let [model (db/get-model "incanter" "incanter" "1.2.3-SNAPSHOT")]
    (is (map? model))
    (is (and (= "incanter" (:name model))
	     (= "incanter" (:group model))
	     (= "1.2.3-SNAPSHOT" (:version model)))))
  (let [model (db/get-model "incanter" "1.2.3-SNAPSHOT")]
    (is (map? model))
    (is (and (= "incanter" (:name model))
	     (= "incanter" (:group model))
	     (= "1.2.3-SNAPSHOT" (:version model)))))
  (let [model (db/get-model "incanter/incanter" "1.2.3-SNAPSHOT")]
    (is (map? model))
    (is (and (= "incanter" (:name model))
	     (= "incanter" (:group model))
	     (= "1.2.3-SNAPSHOT" (:version model))))))

(deftest test-get-all-versions-of-model
  (let [model-versions #{"1.0-new-SNAPSHOT" "1.0-master-SNAPSHOT"
			 "1.2.3-SNAPSHOT" "1.2.1" "1.2.2-SNAPSHOT"
			 "1.2.1-SNAPSHOT" "1.2.2" "0.9.0"
			 "1.0-SNAPSHOT"}]
    (let [models (db/get-all-versions-of-model "incanter" "incanter")]
     (is (and (coll? models) (= 9 (count models))))
     (is (= model-versions (set (map :version models)))))
    (let [models (db/get-all-versions-of-model "incanter")]
      (is (and (coll? models) (= 9 (count models))))
      (is (= model-versions (set (map :version models)))))
    (let [models (db/get-all-versions-of-model "incanter/incanter")]
      (is (and (coll? models) (= 9 (count models))))
      (is (= model-versions (set (map :version models)))))))

(deftest test-search-repo
  (let [search-results (db/search-repo "incanter")]
    (is (= 65 (count search-results)))
    (is (= (set (map :group search-results))
	   #{"incanter" "incanter-latex"}))))

(deftest test-get-models-by-group
  (let [models (db/get-models-by-group "incanter")]
    (is (= 63 (count models)))
    (is (= #{"incanter"} (set (map :group models))))))

