VFSJFileChooser
==========================


IMPORTANT : About the Commons-VFS jars
--------------------------------------

The commons-vfs jars distributed with VFSJFileChooser are patched.

The commons-vfs core jar include patches submitted to commons-vfs
which have not been included yet :
- URL redirection(https://issues.apache.org/jira/browse/VFS-194)
- WebDAV issues(https://issues.apache.org/jira/browse/VFS-74)

The commons-vfs sandbox jar remove the webdav support which is provided
by webdavclient4j(http://webdavclient4j.sf.net). Webdav classes have been
deleted manually from the built jar. 

The commons vfs jar provides usual protocols(FTP, HTTP, etc.)
The commons-vfs sandbox jar provides SMB support.

How to patch Commons-VFS if building it from SVN(trunk)
-------------------------------------------------------
A patch file is distributed with VFSJFileChooser, it is called commons-vfs.patch.
It is located at the root of VFSJFileChooser folder.

At the root of your commons-vfs trunk folder, run the following command:
patch -p0 -i commons-vfs.patch

Under Windows you'll need to download the patch utility :
http://gnuwin32.sourceforge.net/packages/patch.htm

Help/Questions/Issues
------------------------
Please visit the VFSJFileChooser forums : 
http://sourceforge.net/forum/?group_id=215380