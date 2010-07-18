(ns ^{:doc "Teglon Web client library"
      :author "David Edgar Liebke"}
  teglon.client.core
  (:require [clojure.string :as s])
  (:use [clojure.contrib.json :only (read-json)]
	[clojure-http.client :only [request]]))

(def *base-uri* "/api/v1/json")
(def *teglon-server* (ref "http://localhost:8080"))

(defn teglon-server
  ([] @*teglon-server*)
  ([server] (dosync (ref-set *teglon-server* server))))

(defn json-request [url]
  (let [resp (request url)
	result (apply read-json (:body-seq resp))]
    result))

(defn show-model
  ([group name version & [server]]
     (let [server (or server (teglon-server))
	   url (str server *base-uri*
		    "/model/show/" group "/"
		    name "/" version)]
       (json-request url))))

(defn show-models
  ([& [server]]
     (let [server (or server (teglon-server))
	   url (str server *base-uri*
		    "/models/show")]
       (json-request url))))

(defn show-versions
  ([group name & [server]]
     (let [server (or server (teglon-server))
	   url (str server *base-uri*
		    "/versions/show/" group "/" name)]
       (json-request url))))

(defn show-group
  ([group & [server]]
     (let [server (or server (teglon-server))
	   url (str server *base-uri*
		    "/group/show/" group)]
       (json-request url))))

(defn show-model-children
  ([group name version & [server]]
     (let [server (or server (teglon-server))
	   url (str server *base-uri*
		    "/children/model/show/" group "/"
		    name "/" version)]
       (json-request url))))

(defn show-versions-children
  ([group name & [server]]
     (let [server (or server (teglon-server))
	   url (str server *base-uri*
		    "/children/versions/show/" group "/"
		    name)]
       (json-request url))))

(defn show-group-children
  ([group & [server]]
     (let [server (or server (teglon-server))
	   url (str server *base-uri*
		    "/children/group/show/" group)]
       (json-request url))))