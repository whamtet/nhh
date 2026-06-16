#!/usr/bin/env -S bb --classpath .
(ns hello
  (:require adapter))

(adapter/send-response
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (pr-str adapter/req)})
