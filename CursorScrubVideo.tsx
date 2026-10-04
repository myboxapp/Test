import * as React from "react"
import { useEffect, useRef, useState } from "react"
import { addPropertyControls, ControlType, RenderTarget } from "framer"

/**
 * CursorScrubVideo
 *
 * Renders a video whose playhead is driven by the cursor position.
 *
 * README — encoding for buttery, frame-accurate scrubbing
 * -------------------------------------------------------
 * The uploaded video MUST be encoded with every frame as a keyframe
 * (all-intra). Otherwise the browser has to decode from the previous
 * keyframe on every seek and scrubbing will stutter. Recommended:
 *
 *   ffmpeg -i in.mp4 -c:v libx264 -preset slow -crf 18 -g 1 -keyint_min 1 \
 *     -x264-params "scenecut=0" -profile:v high -pix_fmt yuv420p \
 *     -movflags +faststart -an out.mp4
 *
 * @framerSupportedLayoutWidth any
 * @framerSupportedLayoutHeight any
 * @framerIntrinsicWidth 480
 * @framerIntrinsicHeight 480
 * @framerDisableUnlink
 */

type Props = {
    videoFile?: string
    axis: "horizontal" | "vertical"
    reverse: boolean
    trackingArea: "component" | "window"
    smoothing: number
    objectFit: "cover" | "contain" | "fill"
    showPoster: boolean
    borderRadius: number
    style?: React.CSSProperties
}

const clamp01 = (n: number) => Math.min(1, Math.max(0, n))
const EPSILON = 0.008

export default function CursorScrubVideo(props: Props) {
    const {
        videoFile,
        axis = "horizontal",
        reverse = false,
        trackingArea = "component",
        smoothing = 0.22,
        objectFit = "cover",
        showPoster = true,
        borderRadius = 0,
        style,
    } = props

    const rootRef = useRef<HTMLDivElement>(null)
    const videoRef = useRef<HTMLVideoElement>(null)
    const [ready, setReady] = useState(false)

    // Mutable state used by the RAF loop (kept out of React state on purpose).
    const posRef = useRef(0) // normalized cursor position 0..1 (before reverse)
    const currentTimeRef = useRef(0) // lerped time
    const seekingRef = useRef(false)
    const readyRef = useRef(false)
    const liveRef = useRef({ axis, reverse, smoothing })
    liveRef.current = { axis, reverse, smoothing }

    const isCanvas = RenderTarget.current() === RenderTarget.canvas

    // Load / buffer the video, track readiness and seek state.
    useEffect(() => {
        const video = videoRef.current
        if (!video || !videoFile || isCanvas) return

        readyRef.current = false
        seekingRef.current = false
        currentTimeRef.current = 0
        setReady(false)

        const onSeeking = () => (seekingRef.current = true)
        const onSeeked = () => (seekingRef.current = false)
        const onCanPlayThrough = () => {
            if (readyRef.current) return
            video.pause()
            video.currentTime = 0
            currentTimeRef.current = 0
            readyRef.current = true
            setReady(true)
        }

        video.addEventListener("seeking", onSeeking)
        video.addEventListener("seeked", onSeeked)
        video.addEventListener("canplaythrough", onCanPlayThrough)

        video.load()
        // Muted playback is allowed by browsers; this forces frame buffering.
        const p = video.play()
        if (p && typeof p.then === "function") {
            p.then(() => video.pause()).catch(() => {})
        }
        video.currentTime = 0

        return () => {
            video.removeEventListener("seeking", onSeeking)
            video.removeEventListener("seeked", onSeeked)
            video.removeEventListener("canplaythrough", onCanPlayThrough)
            readyRef.current = false
        }
    }, [videoFile, isCanvas])

    // Cursor tracking.
    useEffect(() => {
        if (isCanvas) return
        const root = rootRef.current

        const update = (x: number, y: number, w: number, h: number) => {
            const { axis } = liveRef.current
            const n = axis === "horizontal" ? x / (w || 1) : y / (h || 1)
            posRef.current = clamp01(n)
        }

        if (trackingArea === "window") {
            const onMove = (e: PointerEvent) =>
                update(
                    e.clientX,
                    e.clientY,
                    window.innerWidth,
                    window.innerHeight
                )
            window.addEventListener("pointermove", onMove)
            return () => window.removeEventListener("pointermove", onMove)
        }

        if (!root) return
        // offsetX/Y is relative to the event target (which may be a child),
        // so derive the same value relative to the root via its bounding box.
        const onMove = (e: PointerEvent) => {
            const r = root.getBoundingClientRect()
            update(e.clientX - r.left, e.clientY - r.top, r.width, r.height)
        }
        root.addEventListener("pointermove", onMove)
        return () => root.removeEventListener("pointermove", onMove)
    }, [trackingArea, isCanvas])

    // RAF scrub loop.
    useEffect(() => {
        if (isCanvas) return
        let raf = 0
        const tick = () => {
            raf = requestAnimationFrame(tick)
            const video = videoRef.current
            if (!video || !readyRef.current) return
            const duration = video.duration
            if (!Number.isFinite(duration) || duration <= 0) return

            const { reverse, smoothing } = liveRef.current
            const pos = reverse ? 1 - posRef.current : posRef.current
            const target = pos * duration
            const s = Math.min(1, Math.max(0.02, smoothing))
            const next = currentTimeRef.current + (target - currentTimeRef.current) * s
            currentTimeRef.current = next

            if (
                !seekingRef.current &&
                Math.abs(video.currentTime - next) > EPSILON
            ) {
                video.currentTime = next
            }
        }
        raf = requestAnimationFrame(tick)
        return () => cancelAnimationFrame(raf)
    }, [isCanvas])

    const containerStyle: React.CSSProperties = {
        position: "relative",
        width: "100%",
        height: "100%",
        overflow: "hidden",
        borderRadius,
        touchAction: "none",
        ...style,
    }

    if (!videoFile) {
        return (
            <div
                style={{
                    ...containerStyle,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    background: "rgba(136, 136, 136, 0.15)",
                    color: "#888",
                    font: "500 14px/1.4 Inter, sans-serif",
                    border: "1px dashed rgba(136, 136, 136, 0.5)",
                    boxSizing: "border-box",
                }}
            >
                Add a video file
            </div>
        )
    }

    // "#t=0.001" makes browsers render the first frame as a poster.
    const src = showPoster && !videoFile.includes("#") ? `${videoFile}#t=0.001` : videoFile

    return (
        <div ref={rootRef} style={containerStyle}>
            <video
                ref={videoRef}
                src={src}
                muted
                playsInline
                preload="auto"
                disableRemotePlayback
                style={{
                    display: "block",
                    width: "100%",
                    height: "100%",
                    objectFit,
                    borderRadius,
                    pointerEvents: "none",
                    opacity: showPoster || ready || isCanvas ? 1 : 0,
                    transition: "opacity 0.2s",
                }}
            />
            {!ready && !isCanvas && (
                <div
                    style={{
                        position: "absolute",
                        right: 10,
                        bottom: 10,
                        padding: "3px 8px",
                        borderRadius: 999,
                        background: "rgba(0,0,0,0.5)",
                        color: "#fff",
                        font: "500 11px/1.4 Inter, sans-serif",
                        pointerEvents: "none",
                    }}
                >
                    Loading…
                </div>
            )}
        </div>
    )
}

CursorScrubVideo.defaultProps = {
    axis: "horizontal",
    reverse: false,
    trackingArea: "component",
    smoothing: 0.22,
    objectFit: "cover",
    showPoster: true,
    borderRadius: 0,
    width: 480,
    height: 480,
}

addPropertyControls(CursorScrubVideo, {
    videoFile: {
        type: ControlType.File,
        title: "Video",
        allowedFileTypes: ["video/*"],
        description:
            "Required. For smooth scrubbing, encode with every frame as a keyframe: ffmpeg -i in.mp4 -c:v libx264 -preset slow -crf 18 -g 1 -keyint_min 1 -x264-params \"scenecut=0\" -profile:v high -pix_fmt yuv420p -movflags +faststart -an out.mp4",
    },
    axis: {
        type: ControlType.Enum,
        title: "Axis",
        options: ["horizontal", "vertical"],
        optionTitles: ["Horizontal", "Vertical"],
        defaultValue: "horizontal",
        displaySegmentedControl: true,
    },
    reverse: {
        type: ControlType.Boolean,
        title: "Reverse",
        defaultValue: false,
    },
    trackingArea: {
        type: ControlType.Enum,
        title: "Tracking",
        options: ["component", "window"],
        optionTitles: ["Component", "Window"],
        defaultValue: "component",
        displaySegmentedControl: true,
    },
    smoothing: {
        type: ControlType.Number,
        title: "Smoothing",
        min: 0.02,
        max: 1,
        step: 0.01,
        defaultValue: 0.22,
    },
    objectFit: {
        type: ControlType.Enum,
        title: "Fit",
        options: ["cover", "contain", "fill"],
        optionTitles: ["Cover", "Contain", "Fill"],
        defaultValue: "cover",
    },
    showPoster: {
        type: ControlType.Boolean,
        title: "Poster",
        defaultValue: true,
    },
    borderRadius: {
        type: ControlType.Number,
        title: "Radius",
        min: 0,
        max: 1000,
        unit: "px",
        defaultValue: 0,
    },
})
