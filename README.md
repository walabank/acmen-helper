# acmen-helper
### 还在为繁琐的配置工作和重复的基础Coding而烦恼吗？Acmen-helper一键搞定 。Easy Coding，Enjoy life!

# 最新更新
------
支持生成一个多模块的项目，为生成微服务项目做准备
- xxx-dao 为数据库持久层
- xxx-service为业务逻辑层
- xxx-web为mvc层
- xxx-core为核心依赖及配置（其他模块都要依赖这一个模块）

![image.png](https://upload-images.jianshu.io/upload_images/7220971-71402650b5c418c8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##  背景：
当前每次启动一个新的项目工程，都需要进行复杂而又繁琐的配置工作已经重复的基本代码coding，效率低下。
## 目标：
通过在web端简单配置数据库连接信息，就可以生成一个基于Spring Boot & MyBatis的种子项目，该项目已经集成了基本的配置信息和相关基本操作的RestfulAPI接口。
  其中，配置信息包括:统一API结果封装，统一异常处理，简单签名认证,mybatis和数据源配置；API接口包括针对数据源的所有基本操作。使我们摆脱繁琐无趣的重复工作，专注于业务代码的编写，**减少加班**。
  
  

## 未来

纵向扩展
-----
1.用户自定义多维查询
2.自定义module名称
3.暴露自定义项目生成脚本
4.微服务化-可以生成多模块的项目骨架


横向扩展
--------
1.集成登录模块（web,app端的社交登录，短信登录，验证码登录，密码登录）
2.集成RBAC权限模块


## 核心架构

![image.png](https://upload-images.jianshu.io/upload_images/7220971-bf51c25b99c1dc7c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

  
  
  

