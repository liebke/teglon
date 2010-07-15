(ns teglon.repo-test
  (:require [teglon.repo :as repo])
  (:require [teglon.db :as db])
  (:use [clojure.test]))

;; using a snapshot of clojars.org for testing
(repo/init-repo "/Users/liebke/Desktop/clojars/clojars.org/repo")

(deftest test-list-project-repo-contents
  (is (= 58 (count (repo/list-project-repo-contents "clj-time" "0.1.0-SNAPSHOT"))))
  (is (= 9 (count (repo/list-project-jars "clj-time" "0.1.0-SNAPSHOT"))))
  (is (= 9 (count (repo/list-project-poms "clj-time" "0.1.0-SNAPSHOT")))))

(deftest test-add-to-maven-repo
  (let [jar-file (.getAbsolutePath (last (repo/list-project-jars "clj-time" "0.1.0-SNAPSHOT")))
	pom-file (.getAbsolutePath (last (repo/list-project-poms "clj-time" "0.1.0-SNAPSHOT")))]
    (repo/repository-dir "/tmp/teglon/repo/")
    (repo/add-to-maven-repo jar-file pom-file)
    (let [model (db/get-model "clj-time" "0.1.0-SNAPSHOT")
	  project-dir "clj-time/clj-time/0.1.0-SNAPSHOT/"
	  jar-file-regex (re-pattern
			  (str (repo/repository-dir) project-dir
			       "clj-time-.*\\.jar$"))
	  pom-file-regex (re-pattern
			  (str (repo/repository-dir) project-dir
			       "clj-time-.*\\.pom$"))]
      (is (map? model))
      (is (and (= "clj-time" (:name model))
	       (= "clj-time" (:group model))
	       (= "0.1.0-SNAPSHOT" (:version model))))
      (is (re-matches jar-file-regex
		      (.getAbsolutePath (last (repo/list-project-jars "clj-time" "0.1.0-SNAPSHOT")))))
      (is (re-matches pom-file-regex
		      (.getAbsolutePath (last (repo/list-project-poms "clj-time" "0.1.0-SNAPSHOT"))))))))
