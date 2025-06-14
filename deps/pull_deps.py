import subprocess
import os

def url_replacer(url):
    return url

git='git'

if os.environ.get('DEPS_SOURCE_DIRS','')=='':
    os.environ['DEPS_SOURCE_DIRS']=os.path.join(os.path.dirname(os.path.abspath(__file__)))

deps_dir=os.environ.get('DEPS_SOURCE_DIRS')

def pull(dep_name, repo_url, branch="main"):
    """
    Pull remote repository dependency to local
    Args:
        dep_name (str): Dependency name (will be used as directory name)
        repo_url (str): Repository URL (can be replace by url_replacer, if needed)
        branch (str): Repository branch, defaults to main
    """

    repo_url = url_replacer(repo_url)

    base_dir = deps_dir
    target_dir = os.path.join(base_dir, dep_name)
    
    if os.path.exists(target_dir):
        try:
            # Execute git clone command
            cmd = [
                git, 'pull',
                '--rebase'
            ]
            print(f'[INFO] Update {target_dir}')
            subprocess.run(cmd, check=True,cwd=target_dir)
            print(f"[OK] Successfully pulled {dep_name} @{branch}")
            
        except subprocess.CalledProcessError as e:
            print(f"[ERROR] Failed to pull {dep_name}: {str(e)}")
        except Exception as e:
            print(f"[ERROR] Unexpected error: {str(e)}")
    else:
        try:
            # Execute git clone command
            cmd = [
                git, 'clone',
                '-b', branch,
                '--depth', '1',
                repo_url,
                target_dir
            ]
            print(f'[INFO] Clone into {target_dir}')
            subprocess.run(cmd, check=True)
            print(f"[OK] Successfully pulled {dep_name} @{branch}")
            
        except subprocess.CalledProcessError as e:
            print(f"[ERROR] Failed to pull {dep_name}: {str(e)}")
        except Exception as e:
            print(f"[ERROR] Unexpected error: {str(e)}")
    return target_dir

def main():
    import runpy
    pull('libuv','https://gitee.com/partic/libuv-patched','v1.x')

    targetdir=pull('pwart','https://gitee.com/partic/pwart','main')
    runpy.run_path(os.path.join(targetdir,'deps','pull_deps.py'),run_name='__main__')

    targetdir=pull('PxpRpc','https://gitee.com/partic/PxpRpc','main')
    runpy.run_path(os.path.join(targetdir,'runtime_bridge','deps','pull_deps.py'),run_name='__main__')

    pull('SDL','https://gitee.com/partic/SDL-mirror','release-2.32.x')
    pull('webview','https://gitee.com/partic/webview-mirror.git','master')
    
    pull('txiki.js','https://gitee.com/partic/txiki.js-partic.git','master')

if __name__=='__main__':
    main()