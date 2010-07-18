(defproject teglon "1.0.0-SNAPSHOT"
  :description "Teglon provides Clojure and RESTful APIs for managing, browsing, and searching Maven repositories."
  :dependencies [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
		 ;; needed for teglon.repo
		 [org.apache.maven/maven-ant-tasks "2.0.10"]
                 [org.apache.maven/maven-artifact-manager "2.2.1"]
                 [org.apache.maven/maven-model "2.2.1"]
                 [org.apache.maven/maven-project "2.2.1"]
                 [org.apache.maven.wagon/wagon-file "1.0-beta-6"]
		 ;; needed for teglon.web
		 [compojure "0.4.1"]
		 [ring "0.2.5"]
                 [hiccup "0.4.0-SNAPSHOT"]
		 ;; needed for teglon.github
		 [clojure-http-client "1.1.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :main teglon.main)
