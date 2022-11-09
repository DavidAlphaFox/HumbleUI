(ns io.github.humbleui.ui.image-snapshot
  (:require
    [clojure.math :as math]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas ColorAlphaType Image ImageInfo Surface]
    [java.lang AutoCloseable]))

(core/deftype+ ImageSnapshot [scale child ^:mut ^Image image]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [this ctx rect ^Canvas canvas]
    (let [[sx sy] (if (some? scale)
                    ((juxt :x :y) scale)
                    (let [m44 (.getMat (.getLocalToDevice canvas))]
                      [(nth m44 0) (nth m44 5)]))
          w (int (math/ceil (* (:width rect) sx)))
          h (int (math/ceil (* (:height rect) sy)))]
      (when (and image
              (or 
                (not= (.getWidth image) w)
                (not= (.getHeight image) h)))
        (.close image)
        (set! image nil))
      (when (nil? image)
        (with-open [surface (Surface/makeRaster (ImageInfo/makeS32 w h ColorAlphaType/PREMUL))]
          (core/draw child ctx (core/irect-xywh 0 0 w h) (.getCanvas surface))
          (protocols/-set! this :image (.makeImageSnapshot surface))))
      (.drawImageRect canvas image (core/rect rect))))

  (-event [_ _ctx _event])
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn image-snapshot
  ([child]
   (image-snapshot {} child))
  ([opts child]
   (->ImageSnapshot (:scale opts) child nil)))