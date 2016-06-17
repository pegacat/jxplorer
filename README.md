# JXplorer Version 3.3.1.2 Release Notes
======================================

General Help and Info at the [JXplorer Project Page](http://jxplorer.org/).

Commercial version (reporting, csv import, excel export etc.) at [JXworkbench.com](http://jxworkbench.com/).


# Download Links:

## Extended Commercial version ($10) 

pre-packaged downloads with reporting extensions, csv file support, wildcard find and replace etc.

  [![JXWorkbench downloads](http://jxplorer.org/get_jxw.png)](http://jxworkbench.com/JXWorkBench_Purchase.html)

## Standard Open Source version 

pre-packaged download packages

  [![JXPlorer downloads](http://jxplorer.org/get_jx.png)](http://jxplorer.org/downloads/users.html)

(also available on a trial basis on github, under the [release](https://github.com/pegacat/jxplorer/releases) tab)

## 3.3.1.2

* setting up on github, with current version of code.  (migrating from svn and local project artifacts)
* reintroduced {CRYPT} userpassword formats for legacy directory support - note that CRYPT is not secure, and should not be used if at all possible
* changed handling of tree operation confirmation pop-up to reduce chance of accidental deletions
* moved to java 1.6 for standard build (should still compile under 1.5).
* some messing around with build file to make it more reliable and git friendly
* formatting this file to be .md friendly :-)

## 3.3.1.1

* Defaults to TLS for security; SSL must be manually enabled in config file
* Updated userPassword editor to allow for SHA-256, SHA-512, SSHA-256 and SSHA-512 hashing algorithms
* Updated help with information on new 'advanced userPassword editor'

## 3.3.1.1 (rc3)
 - modified SHA- and SSHA- algorithms to *not* have '-' characters in the algorithm name in the LDAP value:
e.g. "{SSHA51}s234xqw..." instead of "{SSHA-512}s234xqw..." 

## 3.3.1

* Added support for 'read only' connections
* Added a config value 'lock.read.only' which forces *all* connections to be read only
* cosmetic tweaks to logging for rc1

###  LDIF RELEASE!! *

* Added support for LDIF change files
* Added 'preview' for LDIF files to assess changes before importing them into the live directory.



## 3.3.03
* Added 'sorting' to JXWorkBench, allowing for complex queries which sort by attributes other than the DN
* Reports now automatically exported using the report name as the base

## 3.3.02
* Changed display of operational attributes; now included in main table display.
* extended CBSaveLoadTemplate to allow saving UI elements by name, rather than position.
* added $JXOPTS to batch scripts to allow config settings to be passed to batch (e.g. to set config location with -Djxplorer.config)
* work on batch scripts - using java 1.6 classpaths with wildcards
* more work on packaging
* fix for passwords cached within session not correctly filling out connection window in some instances
* support for commercial JXWorkBench plugin (http://jxworkbench.com) - integration with Jasper reporting engine for LDAP reports. 

## 3.3.01
Bug Fixes
* Fixed schema problem with binary syntaxes, mis-identifying some syntaxes
  (e.g. telephonic ones) as binary rather than string
* Correctly saved 'return_attributes.txt' in variable config files location
  (along with usual files such as jxconfig.txt)

## 3.3
Stable Release
* packaged JNDI LDAP test provider (junit directory mock) as part of release
* made 'official' stable release on JXplorer.org downloads page

## 3.3rc1
* JXWorkBench Add on - Support for commercial jxworkbench.com plugin 'applet' as optional add on
  * password vault
  * csv import/export
  * non-indexed & regexp client-side find and replace
* fix for default config files; move to application config directory on Win 7, or if -Djxplorer.config='user.home' used.
* set -Dfile.encoding=utf-8 for consistent x-platform behaviour
* set window title to template name
* included Mock LDAP JNDI provider for JUnit tests (see jxplorer.org/downloads/jndimock.html)
* updated Chinese language (both traditional and simplified) 

## 3.3b6
* fixes for windows 7 installation
* ability to save config in 'natural' platform locations on Win7, OSX if security
  prevents access to install location, or if -Djxplorer.config=home is used.
* save size and divider state of multiple windows on save
* JXPlorer Help files now also on http://jxplorer.org website

## 3.3b5:
* hungarian language file

## 3.3b4:
* add JXworkbench 3rd party plug in to build
* EXPERIMENTAL support for internal node renaming (== move). Not applicable for opendirectory, which will fall back to copy/delete

## 3.3b3:
* fix to language file bug
* allow language to be set in config.

## 3.3b2

New Features:

* Multiple Windows
* Copy-and-Paste between Windows
* Paged Results
* Window name set to data source (directory or ldif file) used. 
* Editing LDIF files with schema.  (2 Step process; first, connect to a directory
with the schema you wish to use.  Then either disconnect or open a new window, and
edit your ldif file)

Bug Fixes:
* numerous problems with naming and multi-valued naming, particularly in LDIF files
* 'copy DN' enabled for LDIF files
* improved tree selection after copy/paste/delete tree events

# CREDITS
=======

Many thanks to our translators who've worked so hard making JXplorer multilingual:

Chinese: For *both* Traditional and Simplified Chinese; Evereasy (Evereasy at gmail.com) -awesome work.

French:   Luc and Serge, and more recently Bruno Haleblian (bruno_haleblian at carrefour.com) for updates.

Hungarian: Richard - many thanks.

And thanks also to many others who wished to remain anonymous, or whom we've lost track of, including
those who did the original French and German translations!

