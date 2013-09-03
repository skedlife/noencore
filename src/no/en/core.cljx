(ns no.en.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace split]]
            #+cljs [goog.crypt.base64 :as base64])
  #+clj (:import [java.net URLEncoder URLDecoder]
                 [org.apache.commons.codec.binary Base64]))

(defn utf8-string
  "Returns `bytes` as an UTF-8 encoded string."
  [bytes]
  #+clj (String. bytes "UTF-8")
  #+cljs (throw (ex-info "Not implemented yet.")))

(defn base64-encode
  "Returns `s` as a Base64 encoded string."
  [s]
  (when s
    #+clj (utf8-string (Base64/encodeBase64 (.getBytes s)))
    #+cljs (base64/encodeString s false)))

(defn base64-decode
  "Returns `s` as a Base64 decoded string."
  [s]
  (when s
    #+clj (utf8-string (Base64/decodeBase64 (.getBytes s)))
    #+cljs (base64/decodeString s false)))

(defn url-encode
  "Returns `s` as an URL encoded string."
  [s & [encoding]]
  (when s
    #+clj (-> (URLEncoder/encode s (or encoding "UTF-8"))
              (replace "%7E" "~")
              (replace "*" "%2A")
              (replace "+" "%20"))
    #+cljs (-> (js/encodeURIComponent s)
               (replace "*" "%2A"))))

(defn url-decode
  "Returns `s` as an URL decoded string."
  [s & [encoding]]
  (when s
    #+clj (URLDecoder/decode s (or encoding "UTF-8"))
    #+cljs (js/decodeURIComponent s)))

(defn- parse-number [s f]
  #+clj (try (f (str s))
             (catch NumberFormatException _ nil))
  #+cljs (let [n (f (str s))]
           (if-not (js/isNaN n) n)))

(defn parse-integer
  "Parse `s` as a integer number."
  [s] (parse-number s #(#+clj Integer/parseInt #+cljs js/parseInt %1)))

(defn parse-query-params
  "Parse `s` as and return the query params in a map."
  [s]
  (if s
    (->> (split (str s) #"&")
         (map #(split %1 #"="))
         (filter #(= 2 (count %1)))
         (mapcat #(vector (keyword (url-decode (first %1))) (url-decode (second %1))))
         (apply hash-map))))

(defn parse-url
  "Parse the URL `s` as and return a Ring compatible map."
  [s]
  (if-let [matches (re-matches #"([^:]+)://(([^:]+):([^@]+)@)?(([^:/]+)(:([0-9]+))?((/[^?]*)(\?(.*))?)?)" (str s))]
    {:scheme (keyword (nth matches 1))
     :user (nth matches 3)
     :password (nth matches 4)
     :server-name (nth matches 6)
     :server-port (parse-integer (nth matches 8))
     :uri (nth matches 10)
     :query-params (parse-query-params  (nth matches 12))
     :query-string (nth matches 12)}))
