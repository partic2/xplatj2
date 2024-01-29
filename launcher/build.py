import io
import os
import subprocess
import shutil

sourceroot=os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
print("source root:"+sourceroot)

buildConfig=dict()


def PrintAndRun(cmd,CaptureOut=False):
    print("execute "+cmd)
    if CaptureOut:
        cp=subprocess.run(cmd,stdout=subprocess.PIPE)
        if cp.returncode!=0:
            raise Exception('subprocess failed');
        return cp.stdout
    else:
        cp=subprocess.run(cmd)
        if cp.returncode!=0:
            raise Exception('subprocess failed');
        return b''


def FindMakeToolChain():
    make=shutil.which('make')
    if make!=None:
        out=PrintAndRun(make+' -v',CaptureOut=True)
        if out.find(b'msys')<0:
            buildConfig['CMAKE_GENERATOR']='Unix Makefiles'
        else:
            buildConfig['CMAKE_GENERATOR']='MSYS Makefiles'
        buildConfig['MAKE']=make
    else:
        make=shutil.which('mingw32-make')
        if make!=None:
            buildConfig['CMAKE_GENERATOR']='MinGW Makefiles'
        buildConfig['MAKE']=make
    assert 'CMAKE_GENERATOR' in buildConfig



def ReadBuildConfig():
    try:
        configFile=io.open_code(sourceroot+'/launcher/build_config.txt')
        exec(configFile.read(),buildConfig)
        del buildConfig['__builtins__']
        configFile.close()
    except Exception as ex:
        print('open build_config.txt failed, use default config.')

def BuildEnvironPrepare():
    if os.environ.get('CMAKE','')!='':
        buildConfig['CMAKE']=os.environ['CMAKE']
    else:
        buildConfig['CMAKE']='cmake'

    if not 'ANDROID_NATIVE_API_LEVEL' in buildConfig:
        buildConfig['ANDROID_NATIVE_API_LEVEL']='21'

    androidHome=os.environ['ANDROID_HOME']
    ndkversion=os.listdir(androidHome+'/ndk')
    for t1 in ndkversion:
        if os.path.exists(os.sep.join([androidHome,'ndk',t1,'build','cmake','android.toolchain.cmake'])):
            #found
            buildConfig['ANDROID_NDK']=os.sep.join([os.environ['ANDROID_HOME'],'ndk',t1])
    assert 'ANDROID_NDK' in buildConfig

    if 'ANDROID_ABI' not in buildConfig:
        buildConfig['ANDROID_ABI']=['armeabi-v7a','arm64-v8a']


def BuildAndroidRelease():
    cmake=shutil.which(buildConfig['CMAKE'])
    assert cmake!=None
    flags=[]
    flags.append('-DANDROID_NATIVE_API_LEVEL={ANDROID_NATIVE_API_LEVEL}'.format(**buildConfig))
    flags.append('-DCMAKE_TOOLCHAIN_FILE={ANDROID_NDK}/build/cmake/android.toolchain.cmake'.format(**buildConfig))
    flags.append('-DCMAKE_BUILD_TYPE=RELEASE')
    flags.append('-G "{CMAKE_GENERATOR}"'.format(**buildConfig))
    flags.append('-S '+sourceroot+'/launcher')
    for abi in buildConfig['ANDROID_ABI']:
        print(f'build android target for {abi}')
        flags2=flags+['-DANDROID_ABI='+abi]
        builddir=os.path.sep.join([sourceroot,'launcher','build','android',abi])
        flags2.append('-B '+builddir)
        PrintAndRun(cmake+' '+' '.join(flags2))
        os.chdir(builddir)
        PrintAndRun(buildConfig['MAKE'])
        sodir=sourceroot+'/launcher/build/android/'+abi+'/build-sdl'
        dstsodir=sourceroot+'/android-project/src/main/jniLibs/'+abi
        os.makedirs(dstsodir,exist_ok=True)
        for sofile in os.listdir(sodir):
            if sofile.endswith('.so'):
                shutil.copy(sodir+'/'+sofile,\
                    dstsodir+'/'+sofile)
        sodir=sourceroot+'/launcher/build/android/'+abi
        shutil.copy(sodir+'/libSDLLoader.so',\
                dstsodir+'/libSDLLoader.so')
    if os.path.exists(sourceroot+'/android-project/src/main/java/org/libsdl'):
        shutil.rmtree(sourceroot+'/android-project/src/main/java/org/libsdl')
    shutil.copytree(sourceroot+'/SDL/android-project/app/src/main/java/org/libsdl',\
        sourceroot+'/android-project/src/main/java/org/libsdl')
    os.chdir(sourceroot+'/android-project')
    gradle=shutil.which('gradle')
    assert gradle!=None
    PrintAndRun(gradle+' assembleRelease')
    shutil.copy(sourceroot+'/android-project/build/outputs/apk/release/xplatj-release.apk',\
        sourceroot+'/launcher/build/xplatj-release.apk')
    os.chdir(sourceroot+'/launcher')

def BuildNativeRelease():
    cmake=shutil.which(buildConfig['CMAKE'])
    assert cmake!=None
    flags=[]
    flags.append('-DCMAKE_BUILD_TYPE=RELEASE')
    flags.append('-S '+sourceroot+'/launcher')
    flags.append('-G "{CMAKE_GENERATOR}"'.format(**buildConfig))
    builddir=os.path.sep.join([sourceroot,'launcher','build','native'])
    flags2=flags+['-B '+builddir]
    PrintAndRun(cmake+' '+' '.join(flags2))
    os.chdir(builddir)
    PrintAndRun(buildConfig['MAKE'])
    os.chdir(sourceroot+'/javase-project')
    gradle=shutil.which('gradle')
    assert gradle!=None
    PrintAndRun(gradle+' distZip')
    os.chdir(sourceroot+'/launcher')
    outdir=sourceroot+'/launcher/build/xplatj-native-release'
    os.makedirs(outdir,exist_ok=True)
    # TODO: what should I copy on linux?
    copyFiles=['launcher','launcher.exe','build-sdl/SDL2.dll','SDLLoader.exe','build-sdl/libSDL2.so','SDLLoader']
    for t1 in copyFiles:
        if os.path.exists(builddir+'/'+t1):
            shutil.copy(builddir+'/'+t1,\
        outdir+'/'+t1.split('/')[-1])
    shutil.unpack_archive(sourceroot+'/javase-project/build/distributions/xplatj.zip',\
        sourceroot+'/launcher/build/native/jse')
    shutil.copytree(sourceroot+'/launcher/build/native/jse/xplatj',outdir,dirs_exist_ok=True)
        
    os.chdir(sourceroot+'/launcher')

if __name__=='__main__':
    ReadBuildConfig()
    BuildEnvironPrepare()
    FindMakeToolChain()
    print(repr(buildConfig))
    if not buildConfig.get('SKIP_ANDROID_BUILD',False):
        BuildAndroidRelease()
    if not buildConfig.get('SKIP_NATIVE_BUILD',False):
        BuildNativeRelease()