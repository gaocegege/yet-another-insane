# Yet Another Insane

YAI是基于[INSANE](https://github.com/colder/insane)的，过程间Control Flow Graph生成器。给定一个scala文件，可以对其进行静态分析，产生相应的过程间控制流图。

## TODO and BUGLIST

1. 如果有print函数，就会报错，不知道使用其他的内置函数会不会有同样的问题，待测试
2. 希望可以添加函数框来表明调用的是什么函数，这样图的可读性会好很多

## Author

* [Ce Gao](https://github.com/gaocegege)
* [Haonan Fu](https://github.com/fhnstephen)
* [Tianyu Xiang](https://github.com/xiangtianyu)