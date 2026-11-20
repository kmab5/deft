"""Rebuild Deft's pinned OFL reader subsets using fontTools + Brotli (project asset tooling)."""
from concurrent.futures import ThreadPoolExecutor
from io import BytesIO
from pathlib import Path
import hashlib
import json
import urllib.parse
import urllib.request
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

GOOGLE_REV = '7af0d90b4d532ffa4b041937b560c6df4e7b1e95'
DYSLEXIC_REV = '1824da5c0e41dc3e13ffc7f3a636dcaf695d61b7'
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/assets'
CACHE = ROOT / 'build/font-sources'
FAMILIES = [('Literata','literata'), ('Source Serif 4','sourceserif4'), ('PT Serif','ptserif'),
 ('EB Garamond','ebgaramond'), ('Crimson Pro','crimsonpro'), ('Bitter','bitter'), ('Lato','lato'),
 ('Open Sans','opensans'), ('Inter','inter'), ('Atkinson Hyperlegible','atkinsonhyperlegible')]
RANGES = [(0,0x024f),(0x0370,0x052f),(0x1e00,0x1fff),(0x2000,0x206f),(0x20a0,0x20cf),
 (0x2c60,0x2c7f),(0x2de0,0x2dff),(0xa640,0xa7ff),(0xab30,0xab6f),(0xfffd,0xfffd)]
UNICODES = {n for lo,hi in RANGES for n in range(lo,hi+1)}


def fetch(url):
    key = hashlib.sha256(url.encode()).hexdigest()
    path = CACHE / key
    if not path.exists():
        request = urllib.request.Request(url,headers={'User-Agent':'Deft-reader-font-build'})
        path.write_bytes(urllib.request.urlopen(request,timeout=90).read())
    return path.read_bytes()


def rename(font, family, style):
    name = font['name']
    # Distinct internal names avoid every upstream Reserved Font Name.
    families = [entry[0] for entry in FAMILIES] + ['OpenDyslexic']
    family = 'Deft Reader ' + str(families.index(family) + 1).zfill(2)
    changed = {1, 2, 3, 4, 6, 16, 17}
    name.names = [entry for entry in name.names if entry.nameID not in changed]
    for identifier,value in [(1,family),(2,style),(3,family+' '+style),(4,family+' '+style),
                             (6,(family+'-'+style).replace(' ','')),(16,family),(17,style)]:
        name.setName(value,identifier,3,1,0x409)
        name.setName(value,identifier,1,0,0)
    if 'CFF ' in font:
        postscript = (family + '-' + style).replace(' ', '')
        font['CFF '].cff.fontNames = [postscript]
        for top in font['CFF '].cff.topDictIndex:
            top.FamilyName = family
            top.FullName = family + ' ' + style


def write_face(data, family, slug, style):
    font = TTFont(BytesIO(data),recalcTimestamp=False)
    if 'fvar' in font:
        axes={a.axisTag:a.defaultValue for a in font['fvar'].axes}
        axes['wght']=700 if style=='bold' else 400
        if 'ital' in axes: axes['ital']=1 if style=='italic' else 0
        font=instantiateVariableFont(font,axes,inplace=True)
    rename(font,family,style.title())
    options=subset.Options()
    options.hinting=False
    options.recalc_timestamp=False
    options.notdef_outline=True
    options.name_IDs=['*']
    options.name_legacy=True
    options.name_languages=['*']
    cutter=subset.Subsetter(options=options);cutter.populate(unicodes=UNICODES);cutter.subset(font)
    if style=='regular':
        preview=TTFont(BytesIO(data),recalcTimestamp=False)
        if 'fvar' in preview:
            axes={a.axisTag:a.defaultValue for a in preview['fvar'].axes};axes['wght']=400
            preview=instantiateVariableFont(preview,axes,inplace=True)
        rename(preview,family,'Regular')
        cutter=subset.Subsetter(options=options);cutter.populate(text=family+' Aa0123456789');cutter.subset(preview)
        preview.flavor=None;preview.save(ASSETS/'reader-font-previews'/f'{slug}.ttf')
    font.flavor='woff2';font.save(ASSETS/'reader-fonts'/f'{slug}-{style}.woff2')


def google_family(item):
    family,slug=item
    prefix=f'https://raw.githubusercontent.com/google/fonts/{GOOGLE_REV}/ofl/{slug}/'
    api=f'https://api.github.com/repos/google/fonts/contents/ofl/{slug}?ref={GOOGLE_REV}'
    listing=json.loads(fetch(api))
    files=[x['name'] for x in listing if x['name'].endswith('.ttf')]
    sources=[]
    for style in ['regular','italic','bold']:
        variable=[f for f in files if '[' in f and ('Italic' in f)==(style=='italic')]
        static=[f for f in files if f.lower().endswith('-'+style+'.ttf')]
        source=(variable or static)[0]
        url=prefix+urllib.parse.quote(source)
        data=fetch(url);write_face(data,family,slug,style)
        sources.append({'face':style,'url':url,'sha256':hashlib.sha256(data).hexdigest()})
    (ASSETS/'licenses'/f'{slug}-OFL.txt').write_bytes(fetch(prefix+'OFL.txt'))
    return {'family':family,'sources':sources}


def dyslexic():
    prefix=f'https://raw.githubusercontent.com/antijingoist/opendyslexic/{DYSLEXIC_REV}/'
    sources=[]
    for style in ['regular','italic','bold']:
        url=prefix+'compiled/OpenDyslexic-'+style.title()+'.otf'
        data=fetch(url);write_face(data,'OpenDyslexic','opendyslexic',style)
        sources.append({'face':style,'url':url,'sha256':hashlib.sha256(data).hexdigest()})
    (ASSETS/'licenses/opendyslexic-OFL.txt').write_bytes(fetch(prefix+'OFL.txt'))
    return {'family':'OpenDyslexic','sources':sources}


if __name__=='__main__':
    CACHE.mkdir(parents=True,exist_ok=True)
    for path in ['reader-fonts','reader-font-previews','licenses']:(ASSETS/path).mkdir(parents=True,exist_ok=True)
    with ThreadPoolExecutor(max_workers=3) as pool: provenance=list(pool.map(google_family,FAMILIES))
    provenance.append(dyslexic())
    (ROOT/'font-sources.json').write_text(json.dumps(provenance,indent=2)+'\n',encoding='utf-8')
    size=sum(p.stat().st_size for folder in ('reader-fonts','reader-font-previews') for p in (ASSETS/folder).iterdir())
    print(f'Bundled 11 families, regular/italic/bold + name previews: {size:,} bytes')
    assert size<=3_000_000, 'Reader font budget exceeded'
