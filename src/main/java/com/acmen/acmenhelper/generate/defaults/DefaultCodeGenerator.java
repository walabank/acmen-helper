package com.acmen.acmenhelper.generate.defaults;

import com.acmen.acmenhelper.common.RequestHolder;
import com.acmen.acmenhelper.config.ProjectConfig;
import com.acmen.acmenhelper.exception.GlobalException;
import com.acmen.acmenhelper.generate.AbstractCodeGenerator;
import com.acmen.acmenhelper.model.CodeDefinitionDetail;
import com.acmen.acmenhelper.model.DBDefinition;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.*;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.acmen.acmenhelper.model.CodeDefinitionDetail.JAVA_PATH;
import static com.acmen.acmenhelper.model.CodeDefinitionDetail.RESOURCES_PATH;
import static com.acmen.acmenhelper.util.NameConvertUtil.*;

/**
 * 默认代码生成器，SOA单体架构
 * @author gaowenfeng
 * @date 2018/5/16
 */
@Slf4j
public class DefaultCodeGenerator extends AbstractCodeGenerator {

    private final static String LOG_PRE = "代码生成器>";

    @Autowired
    private freemarker.template.Configuration cfg;

    @Autowired
    private ProjectConfig projectConfig;



    @Override
    protected void genConfigCode(CodeDefinitionDetail codeDefinitionDetail) {
        DBDefinition dbDefinition = getDbDefinitionFromSession();
        String projectPath = getProjectPath(codeDefinitionDetail,"web");
        Map<String, Object> data = buildConfigDataMap(codeDefinitionDetail, dbDefinition);

        File ymlDevFile = new File(projectPath + RESOURCES_PATH + "/application-dev.yml");
        generateFtlCode(data,ymlDevFile,"application-dev.ftl");

        File ymlFile = new File(projectPath + RESOURCES_PATH + "/application.yml");
        generateFtlCode(data,ymlFile,"application.ftl");

        projectPath = getProjectPath(codeDefinitionDetail,"core");
        File mybatisConfigFile = new File(projectPath + JAVA_PATH + codeDefinitionDetail.getCorePackage() + "/MybatisConfigurator.java");
        generateFtlCode(data,mybatisConfigFile,"MybatisConfigurator.ftl");
    }


    @Override
    protected void genBaseCode(CodeDefinitionDetail codeDefinitionDetail) throws GlobalException {
        Context context = getContext(codeDefinitionDetail);
        for (String tableName : codeDefinitionDetail.getCodeDefinition().getTableList()) {
            genCodeByCustomModelName(tableName, null,codeDefinitionDetail,context);
        }
    }

    /**
     * 通过数据表名称，和自定义的 Model 名称生成代码
     * 如输入表名称 "t_user_detail" 和自定义的 Model 名称 "User" 将生成 User、UserMapper、UserService ...
     * @param tableName 数据表名称
     * @param modelName 自定义的 Model 名称
     */
    private void genCodeByCustomModelName(String tableName, String modelName,CodeDefinitionDetail codeDefinitionDetail,Context context){
        genModelAndMapper(tableName, modelName,context);
        genServiceAndController(tableName, modelName,codeDefinitionDetail);
    }

    /**
     * 使用freemarker生成service，controller层代码
     * @param tableName
     * @param modelName
     * @param codeDefinitionDetail
     */
    private void genServiceAndController(String tableName, String modelName,CodeDefinitionDetail codeDefinitionDetail) {
        String modelNameUpperCamel = buildModelNameUpperCamel(tableName, modelName);

        log.info(LOG_PRE+modelNameUpperCamel+"-controller/service/impl生成开始");
        try {
            //构建占位符数据
            Map<String,Object> data = buildBaseDataMap(modelNameUpperCamel,tableName,codeDefinitionDetail);

            //生成java类
            String projectWebPath = getProjectPath(codeDefinitionDetail,"web");
            File controllerFile = new File(projectWebPath + JAVA_PATH + codeDefinitionDetail.getControllerPackage() + modelNameUpperCamel + "Controller.java");
            generateFtlCode(data,controllerFile,"controller.ftl");

            //生成server类
            String projectServicePath = getProjectPath(codeDefinitionDetail,"service");
            File serviceFile = new File(projectServicePath+ JAVA_PATH + codeDefinitionDetail.getServicePackage() + modelNameUpperCamel + "Service.java");
            generateFtlCode(data,serviceFile,"service.ftl");

            //生成server.impl类
            File serviceImplFile = new File(projectServicePath+ JAVA_PATH + codeDefinitionDetail.getServiceImplPackage() + modelNameUpperCamel + "ServiceImpl.java");
            generateFtlCode(data,serviceImplFile,"service-impl.ftl");

            log.info(LOG_PRE+modelNameUpperCamel+"-controller/service/impl生成成功");
        } catch (Exception e) {
            throw new GlobalException(1 , "生成controller/service/impl失败" , e);
        }
    }

    /**
     * 使用mybatis生成工具生成dao层代码
     * @param tableName
     * @param modelName
     * @param context
     */
    private void genModelAndMapper(String tableName, String modelName,Context context) {
        TableConfiguration tableConfiguration = new TableConfiguration(context);
        tableConfiguration.setTableName(tableName);
        if (StringUtils.isNotEmpty(modelName)){
            tableConfiguration.setDomainObjectName(modelName);
        }
        tableConfiguration.setGeneratedKey(new GeneratedKey("id", "Mysql", true, null));
        context.addTableConfiguration(tableConfiguration);

        List<String> warnings;
        MyBatisGenerator generator;
        try {
            Configuration config = new Configuration();
            config.addContext(context);
            config.validate();

            boolean overwrite = true;
            DefaultShellCallback callback = new DefaultShellCallback(overwrite);
            warnings = new ArrayList<String>();
            generator = new MyBatisGenerator(config, callback, warnings);
            generator.generate(null);
        } catch (Exception e) {
            throw new GlobalException(1 , "生成Model和Mapper失败", e);
        }

        if (generator.getGeneratedJavaFiles().isEmpty() || generator.getGeneratedXmlFiles().isEmpty()) {
            throw new GlobalException(1 , "生成Model和Mapper失败：" + warnings , null);
        }
        if (StringUtils.isEmpty(modelName)) {
            modelName = tableNameConvertUpperCamel(tableName);
        }
        log.info(LOG_PRE+modelName + ".java 生成成功");
        log.info(LOG_PRE+modelName + "Mapper.java 生成成功");
        log.info(LOG_PRE+modelName + "Mapper.xml 生成成功");
    }

    /**
     * 生成代码文件
     * @param data
     * @param file
     * @param ftlName
     * @return
     */
    private void generateFtlCode(Map<String,Object> data,File file,String ftlName){
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            cfg.getTemplate(ftlName).process(data,
                    new FileWriter(file));
        } catch (Exception e) {
            throw new GlobalException(1 , LOG_PRE+"生成代码文件异常，请重试" , e);
        }
    }


    /**
     * 构建Crud模板的map
     * @param tableName
     * @param modelNameUpperCamel
     * @return
     */
    private Map<String,Object> buildBaseDataMap(String modelNameUpperCamel,String tableName,CodeDefinitionDetail codeDefinitionDetail){
        Map<String, Object> data = Maps.newHashMap();

        data.put("date", codeDefinitionDetail.getCodeDefinition().getAuthor());
        data.put("author", codeDefinitionDetail.getCodeDefinition().getAuthor());

        data.put("baseRequestMapping", modelNameConvertMappingPath(modelNameUpperCamel));
        data.put("modelNameUpperCamel", modelNameUpperCamel);
        data.put("modelNameLowerCamel", tableNameConvertLowerCamel(tableName));

        data.put("basePackage", codeDefinitionDetail.getBasePackage());
        return data;
    }

    /**
     * 根据配置类模板的data
     * @param codeDefinitionDetail
     * @param dbDefinition
     * @return
     */
    private Map<String, Object> buildConfigDataMap(CodeDefinitionDetail codeDefinitionDetail, DBDefinition dbDefinition) {
        Map<String,Object> data = Maps.newHashMap();
        data.put("driver_class",dbDefinition.getDriverClass());
        data.put("url",dbDefinition.getUrl());
        data.put("username",dbDefinition.getUsername());
        data.put("password",dbDefinition.getPassword());
        data.put("coreMapperPath",codeDefinitionDetail.getMapperInterfaceReference());
        data.put("type_aliases_package",codeDefinitionDetail.getModulePackage());

        data.put("date", codeDefinitionDetail.getCodeDefinition().getAuthor());
        data.put("author", codeDefinitionDetail.getCodeDefinition().getAuthor());
        data.put("basePackage", codeDefinitionDetail.getBasePackage());
        return data;
    }

    /**
     * 获取Mybatis上下文
     * @return
     */
    private Context getContext(CodeDefinitionDetail codeDefinitionDetail) {
        DBDefinition dbDefinition = getDbDefinitionFromSession();
        String projectPath = getProjectPath(codeDefinitionDetail,"dao");
        Context context = new Context(ModelType.FLAT);
        context.setId("Potato");
        context.setTargetRuntime("MyBatis3Simple");
        context.addProperty(PropertyRegistry.CONTEXT_BEGINNING_DELIMITER, "`");
        context.addProperty(PropertyRegistry.CONTEXT_ENDING_DELIMITER, "`");

        JDBCConnectionConfiguration jdbcConnectionConfiguration = new JDBCConnectionConfiguration();
        jdbcConnectionConfiguration.setConnectionURL(dbDefinition.getUrl());
        jdbcConnectionConfiguration.setUserId(dbDefinition.getUsername());
        jdbcConnectionConfiguration.setPassword(dbDefinition.getPassword());
        jdbcConnectionConfiguration.setDriverClass(dbDefinition.getDriverClass());
        context.setJdbcConnectionConfiguration(jdbcConnectionConfiguration);

        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setConfigurationType("tk.mybatis.mapper.generator.MapperPlugin");
        pluginConfiguration.addProperty("mappers", codeDefinitionDetail.getMapperInterfaceReference());
        context.addPluginConfiguration(pluginConfiguration);

        JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = new JavaModelGeneratorConfiguration();
        javaModelGeneratorConfiguration.setTargetProject(projectPath + JAVA_PATH);
        javaModelGeneratorConfiguration.setTargetPackage(codeDefinitionDetail.getModulePackage());
        context.setJavaModelGeneratorConfiguration(javaModelGeneratorConfiguration);

        SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration = new SqlMapGeneratorConfiguration();
        sqlMapGeneratorConfiguration.setTargetProject(projectPath + RESOURCES_PATH);
        sqlMapGeneratorConfiguration.setTargetPackage("mapper");
        context.setSqlMapGeneratorConfiguration(sqlMapGeneratorConfiguration);

        JavaClientGeneratorConfiguration javaClientGeneratorConfiguration = new JavaClientGeneratorConfiguration();
        javaClientGeneratorConfiguration.setTargetProject(projectPath + JAVA_PATH);
        javaClientGeneratorConfiguration.setTargetPackage(codeDefinitionDetail.getMapperPackage());
        javaClientGeneratorConfiguration.setConfigurationType("XMLMAPPER");
        context.setJavaClientGeneratorConfiguration(javaClientGeneratorConfiguration);
        return context;
    }

    private String getProjectPath(CodeDefinitionDetail codeDefinitionDetail,String module) {
        String projectPath = null;
        if("true".equals(projectConfig.getIsSpiltModule())){
            projectPath = codeDefinitionDetail.getProjectPath()+"/"+codeDefinitionDetail.getCodeDefinition().getArtifactId()+"-"+module;
        }else {
            projectPath = codeDefinitionDetail.getProjectPath();
        }
        return projectPath;
    }

    /**
     * 创建model名称
     * @param tableName
     * @param modelName
     * @return
     */
    private String buildModelNameUpperCamel(String tableName, String modelName){
        return StringUtils.isEmpty(modelName) ? tableNameConvertUpperCamel(tableName) : modelName;
    }

    /**
     * 获取session
     * @return
     */
    private DBDefinition getDbDefinitionFromSession(){
        DBDefinition dbDefinition = null;
        try {
            dbDefinition = (DBDefinition) RequestHolder.getCurrentRequest().getSession().getAttribute("dbDefinition");
        } catch (Exception e) {
            throw new GlobalException(1 , LOG_PRE+"从session中获取DBDefinition失败",e);
        }
        return dbDefinition;
    }

}
