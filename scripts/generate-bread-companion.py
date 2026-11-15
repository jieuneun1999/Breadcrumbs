"""Offline transcription of the owner's RGB PNG into visible ASCII symbols."""
from pathlib import Path
import hashlib
import struct
import sys
import zlib

REFERENCE = Path(sys.argv[1])
ROOT = Path(__file__).resolve().parents[1]
COMPACT = '--compact' in sys.argv[2:]
WIDTH, HEIGHT = (28, 10) if COMPACT else (64, 22)
CROP = (300, 145, 1260, 790) if COMPACT else (290, 135, 1275, 795)
CLASS = 'ReferenceCompactBreadArt' if COMPACT else 'ReferenceBreadArt'
PALETTE = [0, 0x74401e, 0x95501d, 0xb56821, 0xce8528, 0xe39b32, 0xf2b545,
           0xffc85c, 0xffd777, 0xffe299, 0xffedb8, 0xfff2ce,
           0xcc7274, 0xe78f8d, 0xf3aba3, 0x54301c]
DISPLAY_PALETTE = [0, 8605996, 10642219, 12351536, 14193977, 15248966, 16040540, 16765562, 16768924, 16771512, 16773326, 16774623, 13926533, 14982042, 15841196, 6962213]
DENSITY = ' .,:;+*oO#@'


def decode(data):
    width,height,depth,kind,_,_,interlace = struct.unpack('>IIBBBBB', data[16:29])
    if depth != 8 or kind != 2 or interlace:
        raise ValueError('Expected the supplied noninterlaced RGB8 reference')
    if (width,height)!=(1696,927): raise ValueError("Reference dimensions changed")
    payload = bytearray(); at = 8
    while at < len(data):
        size = struct.unpack('>I', data[at:at+4])[0]
        if data[at+4:at+8] == b'IDAT': payload.extend(data[at+8:at+8+size])
        at += size+12
    stride=width*3; decoder=zlib.decompressobj(); raw=decoder.decompress(payload,height*(stride+1)+1)
    if len(raw)!=height*(stride+1) or not decoder.eof: raise ValueError("Reference PNG payload size")
    previous=bytearray(stride); rows=[]
    for y in range(height):
        mode=raw[y*(stride+1)]; row=bytearray(raw[y*(stride+1)+1:(y+1)*(stride+1)])
        for x in range(stride):
            left=row[x-3] if x>=3 else 0; above=previous[x]; corner=previous[x-3] if x>=3 else 0
            if mode==1: predictor=left
            elif mode==2: predictor=above
            elif mode==3: predictor=(left+above)//2
            elif mode==4:
                p=left+above-corner; a,b,c=abs(p-left),abs(p-above),abs(p-corner)
                predictor=left if a<=b and a<=c else above if b<=c else corner
            elif mode==0: predictor=0
            else: raise ValueError('Unsupported PNG filter')
            row[x]=(row[x]+predictor)&255
        rows.append(row); previous=row
    return width,height,rows


def main():
    data=REFERENCE.read_bytes(); width,height,pixels=decode(data)
    if (width,height)!=(1696,927): raise ValueError('Expected the approved mascot reference dimensions')
    left,top,right,bottom=CROP; marks=[]; inks=[]
    for y in range(HEIGHT):
        glyphs=[]; bands=[]
        y0=top+(bottom-top)*y//HEIGHT; y1=top+(bottom-top)*(y+1)//HEIGHT
        for x in range(WIDTH):
            x0=left+(right-left)*x//WIDTH; x1=left+(right-left)*(x+1)//WIDTH
            samples=[]; total=0; lit=0
            for py in range(y0,y1,2):
                for px in range(x0,x1,2):
                    total+=1; r,g,b=pixels[py][px*3:px*3+3]
                    if r>45 and r>g*1.025:
                        samples.append((r,g,b))
                        if (r*3+g*6+b)//10>58: lit+=1
            coverage=lit/max(1,total)
            if not samples or coverage<.055:
                glyphs.append(' ');bands.append('0');continue
            rgb=tuple(sum(c[i] for c in samples)//len(samples) for i in range(3))
            rose=rgb[2]>rgb[1]*.78 and rgb[0]>rgb[1]*1.35
            allowed=range(12,15) if rose else range(1,12)
            band=min(allowed,key=lambda i:sum((rgb[c]-((PALETTE[i]>>(16-c*8))&255))**2 for c in range(3)))
            if not rose: band=min(11,band+1)
            level=min(10,max(1,round(coverage*10)))
            glyph='/' if rose else DENSITY[level]
            # A controlled texture retains individual symbols on bright surfaces.
            if level>=8 and (x+y*3)%7==0: glyph='O'
            glyphs.append(glyph);bands.append(format(band,'x'))
        marks.append(''.join(glyphs));inks.append(''.join(bands))
    quote=lambda rows:',\n'.join('        "'+row.replace('\\','\\\\').replace('"','\\"')+'"' for row in rows)
    java='''package breadcrumbs.cli;

/** Fixed visible-symbol transcription. No image is loaded at runtime. */
final class CLASS_NAME {
    static final String REFERENCE_SHA256 = "%s";
    static final int[] PALETTE = {%s};
    static final String[] MARKS = {
%s
    };
    static final String[] COLORS = {
%s
    };
    private CLASS_NAME() { }
}
'''%(hashlib.sha256(data).hexdigest(),','.join('0x%06x'%c for c in DISPLAY_PALETTE),quote(marks),quote(inks))
    (ROOT/('src/breadcrumbs/cli/'+CLASS+'.java')).write_text(java.replace('CLASS_NAME',CLASS))
    print('\n'.join(marks))


if __name__=='__main__':main()
