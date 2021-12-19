# HeadSystem
Solution for 1.17 heads bug,

Hi everyone, I have a solution for player heads bug on version 1.17. It is only 1 class and is quite easy to use.


## Usage:
- Player head:
```
String name = "your value"; //Player name
HeadManager.convert(HeadManager.HeadType.PLAYER_HEAD, name); //Returns ItemStack
```

- Base64 head:
```
String name = "your value"; //Base64 code
HeadManager.convert(HeadManager.HeadType.BASE64, name); //Returns ItemStack
```


Thanks to itIsMaku for kotlin version <3 (Kotlin version will be rewriten to 1.1 soon)
