(ns ^{:doc "Functions for managing OpenID authentication."
      :author "David Edgar Liebke"}
  teglon.web.openid
  (:import [org.expressme.openid OpenIdManager Base64 Endpoint]
	   javax.crypto.spec.SecretKeySpec
	   javax.crypto.Mac
	   [java.io ByteArrayInputStream StringWriter])
  (:require [teglon.config :as config]
	    [teglon.security :as sec]
	    [clojure.string :as s]
	    [clojure.contrib.logging :as log]))


(def *default-google-alias* "ext1")
(def *default-yahoo-alias* "ax")

(defn get-alias [provider]
  (condp = provider
      "Google" *default-google-alias*
      "Yahoo" *default-yahoo-alias*
      *default-google-alias*))

(defn get-openid-manager []
  (doto (OpenIdManager.)
    (.setReturnTo config/*openid-return-to-url*)
    (.setRealm config/*openid-return-to-realm*)))

(def get-openid-endpoint
     (memoize (fn [openid-manager provider]
		(.lookupEndpoint openid-manager provider))))

(def get-openid-association
     (memoize (fn [openid-manager provider]
		(.lookupAssociation openid-manager
				    (get-openid-endpoint openid-manager
							 provider)))))

(def get-raw-mac-key
     (memoize (fn [openid-manager provider]
		(.getRawMacKey (get-openid-association openid-manager provider)))))

(defn get-authentication-url [openid-manager provider]
  (.getAuthenticationUrl openid-manager
			 (get-openid-endpoint openid-manager provider)
			 (get-openid-association openid-manager provider)))

(defn get-hmac-sha1 [data-string key]
  (let [signing-key (SecretKeySpec. key "HmacSHA1")
	mac (Mac/getInstance "HmacSHA1")]
    (.init mac signing-key)
    (->> (.getBytes data-string "UTF-8")
	 (.doFinal mac)
	 Base64/encodeBytes)))

(defn get-authentication
  ([request key alias]
     (let [params (:params request)
	   identity (params "openid.identity")
	   invalidate-handle? (params "openid.invalidate_handle")
	   sig (params "openid.sig")
	   signed (params "openid.signed")
	   return-to (params "openid.return_to")
	   signed-params (s/split signed #"[\\,]+")
	   sb (apply str (for [p signed-params]
			   (str p ":" (params (str "openid." p)) "\n")))
	   hmac (get-hmac-sha1 sb key)]
       (when (= sig hmac)
	 {:identity identity
	  :email (params (str "openid." alias ".value.email"))
	  :language (params (str "openid." alias ".value.language"))
	  :gender (params (str "openid." alias ".value.gender"))
	  :first-name (params (str "openid." alias ".value.firstname"))
	  :last-name (params (str "openid." alias ".value.lastname"))}))))

(defn openid-redirect-handler [provider]
  (let [manager (get-openid-manager)
	redirect-url (-> manager
			 (get-authentication-url provider))
	openid-key (get-raw-mac-key manager provider)
	openid-alias (get-alias provider)]
    (log/info (str (java.util.Date.) ": Redirecting to " provider " for OpenID authentication"))
    {:status 302
     :session {:openid-key openid-key
	       :openid-alias openid-alias
	       :openid-provider provider}
     :headers {"Location" redirect-url}}))

(defn openid-auth-handler
  ([request]
     (let [openid-key (-> request :session :openid-key)
	   openid-alias (-> request :session :openid-alias)
	   openid-provider (-> request :session :openid-provider)
	   authentication (get-authentication request openid-key openid-alias)
	   openid-identity-token (sec/get-sha1-string (:identity authentication))]
       (log/info (str (java.util.Date.) ": Received OpenID authentication from "
		      openid-provider " for email address " (:email authentication)))
       (log/info (str (java.util.Date.) ": Writing OpenID identification token to cookie "
		      openid-identity-token))
       {:status 200
	:headers {"Content-Type" "text/html"}
	:body (prn-str authentication)
	:cookies {:openid-identity openid-identity-token}})))

(defn openid-handler [request]
  (let [query-params (:query-params request)
	provider (query-params "op")]
    (cond
     (empty? query-params)
       {:status 302 :headers {"Location" "/"}}
     provider
       (openid-redirect-handler provider)
     :else
       (openid-auth-handler request))))