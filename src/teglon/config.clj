(ns ^{:doc "Teglon configuration settings"
      :author "David Edgar Liebke"}
  teglon.config)

;; These values are needed for OpenID authentication.
(def *base-url* "http://localhost:8080")
;; (def *return-to-url* "http://173.79.53.104/openid")
(def *openid-return-to-url* (str *base-url* "/openid"))
;; (def *realm* "http://173.79.53.104")
(def *openid-return-to-realm* *base-url*)

