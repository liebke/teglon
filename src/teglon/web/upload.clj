(ns ^{:doc "Functions used to upload jar and pom files to Teglon."
      :author "David Edgar Liebke"}
  teglon.web.upload
  (:require [teglon.repo :as repo]
	    [teglon.web.html :as html]))

(defn upload-file-to-repo [request]
   (let [pom-file (-> request :multipart-params (get "pom-file"))
	 pom-tmp-file (:tempfile pom-file)
	 jar-file (-> request :multipart-params (get "jar-file"))
	 jar-tmp-file (:tempfile jar-file)
	 model (repo/add-to-maven-repo jar-tmp-file pom-tmp-file)]
     (html/upload-page model (:filename pom-file) (:filename jar-file))))
