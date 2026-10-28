import * as React from 'react';
import {useEffect, useRef, useState} from 'react';
import {StyleSheet, Text, View, useWindowDimensions} from 'react-native';
import {useTVEventHandler} from '@amazon-devices/react-native-kepler';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {Video} from '@amazon-devices/react-native-w3cmedia';
import {DEFAULT_LEVEL, LEVELS, clampLevel, formatTime, grey, step} from './drift';

// Black screen with a small dim clock drifting slowly, for sleeping with audio on.
// Up/Down set the brightness (remembered); Back leaves (system default).
const FRAME_MS = 1000; // once a second: the clock ticks and moves, near-zero CPU
const HINT_MS = 8000; // how long the help line shows after opening or a change
const KEY = 'nivel';
// Vega only holds off the screensaver for an app that is playing video, and gives other apps no API for it
// (user_engagement needs an Amazon-only permission). So a silent, all-black one-hour video plays under the clock.
const KEEP_AWAKE = 'file:///pkg/assets/raw/black.mp4';

export const App = () => {
  const {width, height} = useWindowDimensions();
  const [level, setLevel] = useState(DEFAULT_LEVEL);
  const [hintUntil, setHintUntil] = useState(Date.now() + HINT_MS);
  const [now, setNow] = useState(new Date());
  const [pos, setPos] = useState({x: width * 0.4, y: height * 0.45});
  const vel = useRef({x: 3, y: 2});
  const size = useRef({w: 0, h: 0});
  const video = useRef<Video>(null);

  useEffect(() => {
    AsyncStorage.getItem(KEY)
      .then(v => v !== null && setLevel(clampLevel(Number(v))))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const t = setInterval(() => {
      setNow(new Date());
      setPos(p => {
        const [x, vx] = step(p.x, vel.current.x, width - size.current.w);
        const [y, vy] = step(p.y, vel.current.y, height - size.current.h);
        vel.current = {x: vx, y: vy};
        return {x, y};
      });
    }, FRAME_MS);
    return () => clearInterval(t);
  }, [width, height]);

  useTVEventHandler(evt => {
    if (evt.eventKeyAction !== 0) return; // presses only, not releases
    const d = evt.eventType === 'up' ? 1 : evt.eventType === 'down' ? -1 : 0;
    if (d === 0) return;
    setLevel(l => {
      const next = clampLevel(l + d);
      AsyncStorage.setItem(KEY, String(next)).catch(() => {});
      return next;
    });
    setHintUntil(Date.now() + HINT_MS);
  });

  return (
    <View style={styles.root}>
      <Video
        style={StyleSheet.absoluteFill}
        src={KEEP_AWAKE}
        autoplay
        ref={video}
        onEnded={() => {
          // loop is not supported by the Vega player, so restart by hand
          if (video.current) {
            video.current.currentTime = 0;
            video.current.play();
          }
        }}
      />
      <Text
        style={[styles.clock, {color: grey(level), left: pos.x, top: pos.y}]}
        onLayout={e => {
          size.current = {w: e.nativeEvent.layout.width, h: e.nativeEvent.layout.height};
        }}>
        {formatTime(now)}
      </Text>
      {now.getTime() < hintUntil && (
        <Text style={styles.hint}>
          Brilho {level + 1}/{LEVELS.length} · Cima/Baixo ajusta · Voltar sai
        </Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  root: {flex: 1, backgroundColor: '#000000'},
  clock: {position: 'absolute', fontSize: 56, fontWeight: '300'},
  hint: {position: 'absolute', bottom: '6%', width: '100%', textAlign: 'center', fontSize: 20, color: '#2e2e2e'},
});
