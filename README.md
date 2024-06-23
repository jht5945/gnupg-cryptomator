# gnupg-cryptomator

> This project is fork from https://github.com/purejava/keepassxc-cryptomator , add GnuPG support

[![GitHub Release](https://img.shields.io/github/v/release/jht5945/gnupg-cryptomator)](https://github.com/jht5945/gnupg-cryptomator/releases)
[![License](https://img.shields.io/github/license/jht5945/gnupg-cryptomator.svg)](https://github.com/jht5945/gnupg-cryptomator/blob/master/LICENSE)

Plug-in for Cryptomator to store vault passwords with GnuPG encryption.


# Build Project

Requirement:

* JDK 17 or later
* Maven 3.8.4 or later

```shell
mvn package
```

Copy packaged plugin from `target/gnupg-cryptomator-$VERSION.jar` to Cryptomator plugins directory.
> Usage reference [Wiki](https://github.com/purejava/keepassxc-cryptomator/wiki)


# Configuration

Configure file location:

* `/etc/cryptomator/config.json`
* `~/.config/cryptomator/config.json`

```json
{
  "gnuPgCommand": "/usr/local/bin/gpg",
  "keyId": "FFFFFFFFFFFFFFFFFFFF"
}
```
> `gnuPgCommand` GnuPG command path, find it by `which gpg` <br>
> `keyId` is GnuPG key ID, you can find it by `gpg -K` <br>
> `encryptKeyBasePath` is key store path, default value is `$USER_HOME/.config/cryptomator/keys/`


# Documentation

For documentation please take a look at the [Wiki](https://github.com/purejava/keepassxc-cryptomator/wiki).


# Copyright

Copyright (C) 2021-2024 Ralph Plawetzki<br>
Copyright (C) 2024-2024 Hatter Jiang

The Cryptomator logo is Copyright (C) of https://cryptomator.org/ <br>
The KeePassXC logo is Copyright (C) of https://keepassxc.org/
