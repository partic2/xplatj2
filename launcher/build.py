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
        cp=subprocess.run(cmd,stdout=subprocess.PIPE,shell=True)
        if cp.returncode!=0:
            raise Exception('subprocess failed');
        return cp.stdout
    else:
        cp=subprocess.run(cmd,shell=True)
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

    if not buildConfig.get('SKIP_ANDROID_BUILD',False):
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

    if not 'PACK_JAVA_RUNTIME' in buildConfig:
        buildConfig['PACK_JAVA_RUNTIME']=False


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
        shutil.copy(sodir+'/build-pxprpc_rtbridge/libpxprpc_rtbridge.so',\
                dstsodir+'/libpxprpc_rtbridge.so')
        shutil.copy(sodir+'/launcher',\
                dstsodir+'/launcher')
    if os.path.exists(sourceroot+'/android-project/src/main/java/org/libsdl'):
        shutil.rmtree(sourceroot+'/android-project/src/main/java/org/libsdl')
    shutil.copytree(sourceroot+'/SDL/android-project/app/src/main/java/org/libsdl',\
        sourceroot+'/android-project/src/main/java/org/libsdl')
    os.chdir(sourceroot+'/android-project')
    gradle=os.curdir+os.sep+'gradlew'
    PrintAndRun(gradle+' assembleRelease')
    shutil.copy(sourceroot+'/android-project/build/outputs/apk/release/xplatj-release.apk',\
        sourceroot+'/launcher/build/xplatj-release.apk')
    os.chdir(sourceroot+'/launcher')

def BuildDesktopRelease(name,toolchain):
    print('build desktop for '+name+repr(toolchain))
    cmake=shutil.which(buildConfig['CMAKE'])
    assert cmake!=None
    flags=[]
    # Setup compiler environment
    ldpath=set()
    ldpath.add(os.path.dirname(toolchain['CC']))
    ldpath.add(os.path.dirname(toolchain['CXX']))
    ldpathenv='PATH' if 'nt' == os.name else 'LD_LIBRARY_PATH'
    savedldpath=os.environ.get(ldpathenv,'')
    try:
        os.environ[ldpathenv]=os.pathsep.join(ldpath)+os.pathsep+savedldpath
        flags.append('-DCMAKE_BUILD_TYPE=RELEASE')
        flags.append('"-DCMAKE_C_COMPILER={0}"'.format(toolchain['CC']))
        flags.append('"-DCMAKE_CXX_COMPILER={0}"'.format(toolchain['CXX']))
        flags.append('-DXPLATJ_GUESS_TOOLCHAIN_VARIABLE=ON')
        #flags.append(f'-DCMAKE_TOOLCHAIN_FILE={sourceroot}/launcher/guess_by_compiler.toolchain.cmake')
        flags.append('-S '+sourceroot+'/launcher')
        flags.append('-G "{CMAKE_GENERATOR}"'.format(**buildConfig))
        builddir=os.path.sep.join([sourceroot,'launcher','build',name])
        flags2=flags+['-B '+builddir]
        PrintAndRun(cmake+' '+' '.join(flags2))
        os.chdir(builddir)
        PrintAndRun(buildConfig['MAKE'])
        os.chdir(sourceroot+'/javase-project')
        gradle=os.curdir+os.sep+'gradlew'
        PrintAndRun(gradle+' distZip')
        os.chdir(sourceroot+'/launcher')
        outdir=sourceroot+'/launcher/build/'+name+'_release'
        os.makedirs(outdir,exist_ok=True)
        # TODO: what should I copy on linux?
        copyFiles=['launcher','launcher.exe','build-sdl/SDL2.dll','SDLLoader.exe','build-sdl/libSDL2.so','SDLLoader',
                   'build-pxprpc_rtbridge/libpxprpc_rtbridge.dll','build-pxprpc_rtbridge/libpxprpc_rtbridge.so']
        for t1 in copyFiles:
            filename=t1.split('/')[-1].split('\\')[-1];
            if os.path.exists(os.path.join(builddir,t1)):
                shutil.copy(os.path.join(builddir,t1),\
            os.path.join(outdir,filename))
        shutil.unpack_archive(sourceroot+'/javase-project/build/distributions/xplatj.zip',\
            sourceroot+'/launcher/build/_temp/jse')
        shutil.copytree(sourceroot+'/launcher/build/_temp/jse/xplatj',outdir,dirs_exist_ok=True)
        if buildConfig['PACK_JAVA_RUNTIME']:
            if not os.path.exists(outdir+'/rt-java/release'):
                PrintAndRun(toolchain['JLINK']+' --add-modules java.base,java.logging --output '+outdir+'/rt-java')
            scriptFile=''
            with io.open(outdir+'/bin/xplatj','r',encoding='utf-8') as f1:
                scriptFile=f1.read()
            with io.open(outdir+'/bin/xplatj','w',encoding='utf-8') as f1:
                #skip shebang
                shebangEnd=scriptFile.find('\n')
                f1.write(scriptFile[0:shebangEnd]+'\nexport JAVA_HOME=./rt-java\n'+scriptFile[shebangEnd:])
            with io.open(outdir+'/bin/xplatj.bat','r',encoding='utf-8') as f1:
                scriptFile=f1.read()
            with io.open(outdir+'/bin/xplatj.bat','w',encoding='utf-8') as f1:
                f1.write('set JAVA_HOME=./rt-java\n'+scriptFile)
        os.chdir(sourceroot+'/launcher')
    finally:
        os.environ[ldpathenv]=savedldpath
    
def EnsureBuildDeps():
    sdlexisted=os.path.exists(os.path.join(sourceroot,'SDL','CMakeLists.txt'))
    if not sdlexisted:
        gitexec=shutil.which('git')
        assert gitexec!=None
        PrintAndRun(gitexec+' clone https://gitee.com/partic/SDL-mirror.git "'+os.path.join(sourceroot,'SDL')+'" -b release-2.30.x --depth=1 ')

    webviewexisted=os.path.exists(os.path.join(sourceroot,'pxprpc-modules','pxprpc-webview','webview','CMakeLists.txt'))
    if not webviewexisted:
        gitexec=shutil.which('git')
        assert gitexec!=None
        PrintAndRun(gitexec+' clone https://gitee.com/partic/webview-mirror.git "'+os.path.join(sourceroot,'pxprpc-modules','pxprpc-webview','webview')+'" -b master --depth=1 ')

if __name__=='__main__':
    ReadBuildConfig()
    EnsureBuildDeps()
    BuildEnvironPrepare()
    FindMakeToolChain()
    print(repr(buildConfig))
    if not buildConfig.get('SKIP_ANDROID_BUILD',False):
        BuildAndroidRelease()
    if buildConfig.get('DESKTOP_TOOLCHAIN_LIST',None)!=None:
        for name,toolchain in buildConfig['DESKTOP_TOOLCHAIN_LIST'].items():
            BuildDesktopRelease(name,toolchain)