These demo keystore are in Sun's 'jks' java keystore format.

cacerts - the trusted certificates of directories and certificate authorities (e.g. Verisign etc.) used by JXplorer to establish server-authenticated SSL.  The password is the default Sun password 'passphrase'.

clientcerts - the clients own certificates and private keys.  Importing private keys is quite tricky - refer to the sun doco (you can try importing pkcs#8, but it doesn't work reliably).  The password is the default Sun password 'changeit'.