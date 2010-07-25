(ns 
  ^{:doc "Authentication and authorization functions."
    :author "David Edgar Liebke"}
  teglon.security
  (:import java.security.MessageDigest
	   sun.misc.BASE64Encoder))

(defn get-sha1-string [password]
  (let [md (MessageDigest/getInstance "SHA-1")
	base64-encoder (BASE64Encoder.)]
    (.update md (.getBytes password))
    (.encode base64-encoder (.digest md))))