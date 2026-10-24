# Generates the game sounds (flip, swish, buzzer, crowd) as WAV; convert to OGG with ffmpeg into app/src/main/res/raw/snd_*.ogg.
# Usage: python3 tools/gen_sounds.py <out_dir>
import numpy as np, wave, sys, os
SR=44100
out=sys.argv[1]
rng=np.random.default_rng(7)
def save(name,x):
    x=np.clip(x,-1,1); x=(x/ (np.max(np.abs(x))+1e-9) *0.85*32767).astype(np.int16)
    with wave.open(os.path.join(out,name+'.wav'),'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(x.tobytes())
def t(d): return np.arange(int(SR*d))/SR
def lp(x,a):  # one-pole lowpass
    y=np.zeros_like(x); s=0.0
    for i,v in enumerate(x): s+=a*(v-s); y[i]=s
    return y
def env(n,att,rel):
    e=np.ones(n); a=int(att*SR); r=int(rel*SR)
    if a: e[:a]=np.linspace(0,1,a)
    if r: e[-r:]*=np.linspace(1,0,r)**2
    return e
# flip: short woody tick
tt=t(0.07); flip=np.sin(2*np.pi*1800*tt)*np.exp(-tt*90)+0.5*np.sin(2*np.pi*900*tt)*np.exp(-tt*60)+0.25*rng.standard_normal(len(tt))*np.exp(-tt*200)
save('flip',flip)
# swish: rim tap then net swish (band noise sweep)
tt=t(0.75); n=rng.standard_normal(len(tt))
hp=n-lp(n,0.25); sw=lp(hp,0.6)
shape=np.exp(-((tt-0.28)/0.13)**2)
rim=0.35*np.sin(2*np.pi*620*tt)*np.exp(-tt*35)
swish=sw*shape*1.4+rim
save('swish',swish)
# buzzer: harsh square-ish 2 tone
tt=t(1.1); f=196
b=np.sign(np.sin(2*np.pi*f*tt))*0.5+np.sign(np.sin(2*np.pi*f*1.5*tt))*0.25+0.2*np.sin(2*np.pi*f*2*tt)
b=lp(b,0.35)*env(len(tt),0.01,0.15)
save('buzzer',b)
# crowd cheer: many filtered noise voices + swell
tt=t(1.8); c=np.zeros(len(tt))
for k in range(14):
    nn=rng.standard_normal(len(tt)); a=rng.uniform(0.05,0.2)
    v=lp(nn,a)-lp(nn,a/4)
    mod=1+0.4*np.sin(2*np.pi*rng.uniform(2,6)*tt+rng.uniform(0,6))
    c+=v*mod
for k in range(5): # whistles/whoops
    st=rng.uniform(0.2,1.0); d=0.25; m=(tt>st)&(tt<st+d); f0=rng.uniform(900,1600)
    c[m]+=0.6*np.sin(2*np.pi*(f0+800*(tt[m]-st)/d)*(tt[m]-st))*np.sin(np.pi*(tt[m]-st)/d)
swell=np.clip(tt/0.35,0,1)*np.clip((1.8-tt)/0.8,0,1)
save('crowd',c*swell)
