(ns ^{:doc "This namespace is used to launch the Teglon server"
       :author "David Edgar Liebke"}
    teglon.main
  (:use [teglon.web :as web]
	[aleph])
  (:gen-class))

(defn -main
  ([& [repo-dir & [port] :as args]]
     (if port
       (web/start-server repo-dir (Integer/parseInt port))
       (apply web/start-server args))))