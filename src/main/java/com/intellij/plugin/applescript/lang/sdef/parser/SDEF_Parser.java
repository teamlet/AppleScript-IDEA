package com.intellij.plugin.applescript.lang.sdef.parser;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService;
import com.intellij.plugin.applescript.lang.ide.sdef.DictionaryInfo;
import com.intellij.plugin.applescript.lang.sdef.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.*;
import com.intellij.xml.util.IncludedXmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SDEF_Parser {

  private static final Logger LOG = Logger.getInstance("#" + SDEF_Parser.class.getName());

  public static void parse(@NotNull XmlFile file, @NotNull ApplicationDictionary parsedDictionary) {
    System.out.println("Start parsing xml file --- " + file.toString() + " ---");
    LOG.debug("Start parsing xml file --- " + file.toString() + " ---");

    if (parsedDictionary.getRootTag() == null) {
      parsedDictionary.setRootTag(file.getRootTag());
    }
    final XmlDocument document = file.getDocument();
    if (document != null) {
      final XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlAttribute attr = rootTag.getAttribute("title");
        if ("dictionary".equals(rootTag.getName()) && attr != null) {
          String dicTitle = attr.getValue();
          if (!StringUtil.isEmpty(dicTitle)) {
            parsedDictionary.setName(dicTitle);
          }
        }
        parseRootTag(parsedDictionary, rootTag);
      }
    }
    System.out.println("parsing completed for file.");
    LOG.debug("parsing completed for file.");
  }

  public static void parseRootTag(@NotNull ApplicationDictionary parsedDictionary, @NotNull XmlTag rootTag) {
    String xInclNs = rootTag.getAttributeValue("xmlns:xi");
    XmlTag[] includes = getIncludes(rootTag, xInclNs);
    processIncludes(parsedDictionary, includes);
    XmlTag[] rootSubTags = rootTag.getSubTags();
    for (XmlTag suiteTag : rootSubTags) {
      includes = getIncludes(suiteTag, xInclNs);
      processIncludes(parsedDictionary, includes);
    }
    for (XmlTag suiteTag : rootSubTags) {
      if ("dictionary".equals(suiteTag.getName()) && suiteTag instanceof IncludedXmlTag) {
        XmlFile xmlFile = getDictionaryFileFromInclude(parsedDictionary.getProject(), (IncludedXmlTag) suiteTag);
        if (xmlFile != null) {
          parsedDictionary.processInclude(xmlFile);
        }
      } else if (!"suite".equals(suiteTag.getName())) continue;

      rootTag.getSubTags()[0].getName();
      Suite suite = parseSuiteTag(suiteTag, parsedDictionary);

      XmlTag[] suiteCommands = suiteTag.findSubTags("command");
      for (XmlTag commandTag : suiteCommands) {
        AppleScriptCommand command = parseCommandTag(commandTag, suite);
        parsedDictionary.addCommand(command);
        suite.addCommand(command);
      }

      XmlTag[] suiteClasses = suiteTag.findSubTags("class");
      for (XmlTag classTag : suiteClasses) {
        AppleScriptClass appleScriptClass = parseClassTag(classTag, suite);
        parsedDictionary.addClass(appleScriptClass);
        suite.addClass(appleScriptClass);
      }
      XmlTag[] suiteValueTypes = suiteTag.findSubTags("value-type");
      for (XmlTag valueTypeTag : suiteValueTypes) {
        AppleScriptClass simpleClass = parseClassTag(valueTypeTag, suite);
        parsedDictionary.addClass(simpleClass);
        suite.addClass(simpleClass);
      }

      XmlTag[] suiteClassExtensions = suiteTag.findSubTags("class-extension");
      for (XmlTag classExtensionTag : suiteClassExtensions) {
        AppleScriptClass appleScriptClass = parseClassExtensionTag(classExtensionTag, parsedDictionary, suite);
        if (appleScriptClass != null) {
          parsedDictionary.addClass(appleScriptClass);
          suite.addClass(appleScriptClass);
        }
      }

      XmlTag[] recordTypeTags = suiteTag.findSubTags("record-type");
      for (XmlTag recordTag : recordTypeTags) {
        DictionaryRecord record = parseRecordTag(recordTag, suite);
        parsedDictionary.addRecord(record);
        suite.addRecord(record);
      }

      XmlTag[] enumerationTags = suiteTag.findSubTags("enumeration");
      for (XmlTag enumerationTag : enumerationTags) {
        DictionaryEnumeration enumeration = parseEnumerationTag(enumerationTag, suite);
        parsedDictionary.addEnumeration(enumeration);
        suite.addEnumeration(enumeration);
      }
      parsedDictionary.addSuite(suite);//todo remove adding the components directly to dictionary (see above)
    }
  }

  @Nullable
  private static XmlFile getDictionaryFileFromInclude(@NotNull Project project, IncludedXmlTag xmlIncludeTag) {
    XmlFile xmlFile = null;
    XmlElement origXmlElement = xmlIncludeTag.getOriginal();
    PsiFile origPsiFile = origXmlElement != null ? origXmlElement.getContainingFile() : null;
    if (origPsiFile instanceof XmlFile) {
      xmlFile = (XmlFile) origPsiFile;
      AppleScriptSystemDictionaryRegistryService dictionaryService = ServiceManager.getService(AppleScriptSystemDictionaryRegistryService
          .class);
      VirtualFile vFile = origPsiFile.getVirtualFile();
      DictionaryInfo dInfo = dictionaryService.getDictionaryInfoByApplicationPath(vFile.getPath());
      if (dInfo != null) {
        File ioFile = dInfo.getDictionaryFile();
        if (ioFile.exists()) {
          vFile = LocalFileSystem.getInstance().findFileByIoFile(ioFile);
          if (vFile == null || !vFile.isValid()) return null;

          PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
          xmlFile = (XmlFile) psiFile;
        }
      }
    }
    return xmlFile;
  }

  private static XmlTag[] getIncludes(@NotNull XmlTag rootTag, String xInclNs) {
    if (xInclNs == null) return null;
    return rootTag.findSubTags("include", xInclNs);
  }

  private static void processIncludes(@NotNull ApplicationDictionary parsedDictionary, @Nullable XmlTag[] includes) {
    if (includes == null) return;
    for (XmlTag include : includes) {
      String hrefIncl = include.getAttributeValue("href");
      if (!StringUtil.isEmpty(hrefIncl)) {
        hrefIncl = hrefIncl.replace("file://localhost", "");
        File includedFile = new File(hrefIncl);
//        ((IncludedXmlTag) suiteTag).getOriginal().getContainingFile();
        //as there is assertion error (java.lang.AssertionError: File accessed outside allowed roots),
        // we are trying to find if the dictionary file for this included dictionary was already generated
        AppleScriptSystemDictionaryRegistryService dictionarySystemRegistry = ServiceManager
            .getService(AppleScriptSystemDictionaryRegistryService.class);
        VirtualFile vFile;
        File ioFile = null;
        DictionaryInfo dInfo = dictionarySystemRegistry.getDictionaryInfoByApplicationPath(includedFile.getPath());
        if (dInfo != null) {
          ioFile = dInfo.getDictionaryFile();
        } else if (includedFile.isFile()) {
          String fName = includedFile.getName();
          int index = fName.lastIndexOf('.');
          fName = index < 0 ? fName : fName.substring(0, index);
          ioFile = dictionarySystemRegistry.getDictionaryFile(fName);
        }
        if (ioFile == null || !ioFile.exists()) ioFile = includedFile;
        if (ioFile.exists()) {
          vFile = LocalFileSystem.getInstance().findFileByIoFile(ioFile);
          if (vFile == null || !vFile.isValid()) continue;

          PsiFile psiFile = PsiManager.getInstance(parsedDictionary.getProject()).findFile(vFile);
          XmlFile xmlFile = (XmlFile) psiFile;
          if (xmlFile != null) {
            parsedDictionary.processInclude(xmlFile);
          }
        }
      }
    }
  }

  private static Suite parseSuiteTag(XmlTag suiteTag, ApplicationDictionary dictionary) {
    //todo add all subtags to the suite here
    Suite result = null;
    String name = suiteTag.getAttributeValue("name");
    String code = suiteTag.getAttributeValue("code");
    String description = suiteTag.getAttributeValue("description");
    String hiddenVal = suiteTag.getAttributeValue("hidden");
    if (name != null && code != null) {
      result = new SuiteImpl(dictionary, code, name, "yes".equals(hiddenVal), description, suiteTag);
    }
    return result;
  }

  private static AppleScriptClass parseClassExtensionTag(XmlTag classExtensionTag, ApplicationDictionary dictionary,
                                                         Suite suite) {
    String parentClassName = classExtensionTag.getAttributeValue("extends");
    AppleScriptClass parentClass = dictionary.findClass(parentClassName);
    String parentClassCode = parentClass != null ? parentClass.getCode() : null;
    String pluralName = classExtensionTag.getAttributeValue("plural");
    //todo parent class code could be NULL!! need to parse included dictionary in this case looks like...
    if (parentClassCode == null && parentClassName != null) {
      int l = parentClassName.length();
      parentClassCode = parentClassName.substring(l >= 4 ? 4 : l - 1);
    }
    if (parentClassName == null || parentClassCode == null) return null;

    List<String> elementNames = initClassElements(classExtensionTag);
    List<String> respondingCommands = initClassRespondingMessages(classExtensionTag);

    final AppleScriptClass classExtension = new DictionaryClass(suite, parentClassName, parentClassCode,
        classExtensionTag, null, elementNames, respondingCommands, pluralName);
    String description = classExtensionTag.getAttributeValue("description");
    classExtension.setDescription(description);

    XmlTag[] propertyTags = classExtensionTag.findSubTags("property");
    List<AppleScriptPropertyDefinition> properties = getPropertiesFromTags(classExtension, propertyTags);
    classExtension.setProperties(properties);
    return classExtension;
  }

  @NotNull
  private static List<AppleScriptPropertyDefinition> getPropertiesFromTags(@NotNull DictionaryComponent classOrRecord,
                                                                           @NotNull XmlTag[] propertyTags) {
    if (!(classOrRecord instanceof AppleScriptClass) && !(classOrRecord instanceof DictionaryRecord))
      return new ArrayList<>(0);

    List<AppleScriptPropertyDefinition> properties = new ArrayList<>(propertyTags.length);
    for (XmlTag propTag : propertyTags) {
      AppleScriptPropertyDefinition property;
      String pName = propTag.getAttributeValue("name");
      String pCode = propTag.getAttributeValue("code");
      String pDescription = propTag.getAttributeValue("description");
      String pType = propTag.getAttributeValue("type");
      if (StringUtil.isEmpty(pType)) {
        XmlTag tType = propTag.findFirstSubTag("type");
        pType = tType != null ? tType.getAttributeValue("type") : null;
      }
      String pAccessType = propTag.getAttributeValue("access");
      AccessType accessType = "r".equals(pAccessType) ? AccessType.R : AccessType.RW;
      if (pName != null && pCode != null && pType != null) {
        property = new DictionaryPropertyImpl(classOrRecord, pName, pCode, pType, pDescription, propTag, accessType);
        properties.add(property);
      }
    }
    return properties;
  }

  private static DictionaryEnumeration parseEnumerationTag(XmlTag enumerationTag, Suite suite) {
    String name = enumerationTag.getAttributeValue("name");
    String code = enumerationTag.getAttributeValue("code");
    if (name == null || code == null) return null;

    String description = enumerationTag.getAttributeValue("description");
    final List<DictionaryEnumerator> enumConstants = new ArrayList<>();
    XmlTag[] enumTags = enumerationTag.findSubTags("enumerator");
    final DictionaryEnumeration enumeration = new DictionaryEnumerationImpl(suite, name, code, description,
        enumerationTag);
    for (XmlTag enumTag : enumTags) {
      DictionaryEnumerator enumConst;
      String eName = enumTag.getAttributeValue("name");
      String eCode = enumTag.getAttributeValue("code");
      String eDescription = enumTag.getAttributeValue("description");
      if (eName != null && eCode != null) {
        enumConst = new DictionaryEnumeratorImpl(enumeration, eName, eCode, eDescription, enumTag);
        enumConstants.add(enumConst);
      }
    }
    enumeration.setEnumerators(enumConstants);
    return enumeration;
  }

  private static DictionaryRecord parseRecordTag(XmlTag recordTag, Suite suite) {
    String name = recordTag.getAttributeValue("name");
    String code = recordTag.getAttributeValue("code");
    if (name == null || code == null) return null;

    String description = recordTag.getAttributeValue("description");
    XmlTag[] propertyTags = recordTag.findSubTags("property");
    final DictionaryRecord record = new DictionaryRecordDefinition(suite, name, code, description, recordTag);
    final List<AppleScriptPropertyDefinition> properties = getPropertiesFromTags(record, propertyTags);
    record.setProperties(properties);//could be zero??
    return record;
  }

  private static AppleScriptClass parseClassTag(XmlTag classTag, Suite suite) {
    String name = classTag.getAttributeValue("name");
    String code = classTag.getAttributeValue("code");
    String pluralName = classTag.getAttributeValue("plural");

    if (name == null || code == null) return null;

    String parentClassName = classTag.getAttributeValue("inherits");
    List<String> elementNames = initClassElements(classTag);
    List<String> respondingCommands = initClassRespondingMessages(classTag);


    final AppleScriptClass aClass = new DictionaryClass(suite, name, code, classTag, parentClassName, elementNames,
        respondingCommands, pluralName);
    String description = classTag.getAttributeValue("description");
    aClass.setDescription(description);
    XmlTag[] propertyTags = classTag.findSubTags("property");
    final List<AppleScriptPropertyDefinition> properties = new ArrayList<>();
    for (XmlTag propTag : propertyTags) {
      String pName = propTag.getAttributeValue("name");
      String pCode = propTag.getAttributeValue("code");
      String pDescription = propTag.getAttributeValue("description");
      String pType = propTag.getAttributeValue("type");
      if (StringUtil.isEmpty(pType)) {
        XmlTag tType = propTag.findFirstSubTag("type");
        pType = tType != null ? tType.getAttributeValue("type") : null;
      }
      String pAccessType = propTag.getAttributeValue("access");
      AccessType accessType = "r".equals(pAccessType) ? AccessType.R : AccessType.RW;
      if (pName != null && pCode != null && pType != null) {
        final AppleScriptPropertyDefinition property = new DictionaryPropertyImpl(aClass, pName, pCode, pType,
            pDescription, propTag, accessType);
        properties.add(property);
      }
    }
    aClass.setProperties(properties);
    return aClass;
  }

  private static List<String> initClassRespondingMessages(XmlTag classTag) {
    XmlTag[] elementNameTags = classTag.findSubTags("responds-to");
    List<String> commandNames = new ArrayList<>();
    for (XmlTag elemTag : elementNameTags) {
      String val = elemTag.getAttributeValue("command");
      if (val != null) {
        commandNames.add(val);
      }
    }
    return commandNames;
  }

  private static List<String> initClassElements(XmlTag classTag) {
    XmlTag[] elementNameTags = classTag.findSubTags("element");
    List<String> elementNames = new ArrayList<>();
    for (XmlTag elemTag : elementNameTags) {
      String val = elemTag.getAttributeValue("type");
      if (val != null) {
        elementNames.add(val);
      }
    }
    return elementNames;
  }

  private static AppleScriptCommand parseCommandTag(XmlTag commandTag, Suite suite) {
    String name = commandTag.getAttributeValue("name");
    String code = commandTag.getAttributeValue("code");
    String description = commandTag.getAttributeValue("description");
    String documentation = commandTag.getSubTagText("documentation");

    if (name == null || code == null) return null;

    final AppleScriptCommand command = new AppleScriptCommandImpl(suite, name, code, commandTag);
    command.setDescription(description);
    command.setDictionaryDoc(documentation);

    XmlTag resultTag = commandTag.findFirstSubTag("result");
    if (resultTag != null) {
      String rType = resultTag.getAttributeValue("type");
      String rDesc = resultTag.getAttributeValue("description");
      if (rType != null) {
        CommandResult commandResult = new CommandResult(rType, rDesc);
        command.setResult(commandResult);
      }
    }

    XmlTag directParam = commandTag.findFirstSubTag("direct-parameter");
    CommandDirectParameter directParameter = null;
    if (directParam != null) {
      XmlAttribute type = directParam.getAttribute("type");
      XmlAttribute requiresAccess = directParam.getAttribute("requires-access");
      XmlAttribute paramDescription = directParam.getAttribute("description");
      XmlAttribute isOptionalAttr = directParam.getAttribute("optional");
      String typeVal = null;
      if (type != null) {
        typeVal = type.getValue();

      } else {//could be sub-tag
        XmlTag typeTag = directParam.findFirstSubTag("type");
        if (typeTag != null) {
          XmlAttribute typeAttr = typeTag.getAttribute("type");
          if (typeAttr == null) {
            XmlTag typeSubTag = typeTag.findFirstSubTag("type");
            typeAttr = typeSubTag != null ? typeSubTag.getAttribute("type") : null;
          }
          typeVal = typeAttr != null ? typeAttr.getValue() : null;
        }
      }
      boolean isOptional = isOptionalAttr != null && "yes".equals(isOptionalAttr.getValue());
      if (typeVal != null) {
        if (paramDescription != null) {
          directParameter = new CommandDirectParameter(command, typeVal, paramDescription.getValue(), isOptional);
        } else {
          directParameter = new CommandDirectParameter(command, typeVal, null, isOptional);
        }
      }
    }
    XmlTag[] parameters = commandTag.findSubTags("parameter");
    List<CommandParameter> commandParameters = new ArrayList<>();
    CommandParameter commandParameter = null;
    for (XmlTag paramTag : parameters) {
      String pName = paramTag.getAttributeValue("name");
      String pCode = paramTag.getAttributeValue("code");
      String pDescription = paramTag.getAttributeValue("description");
      String pType = paramTag.getAttributeValue("type");
      if (pType == null) {
        XmlTag typeSubTag = paramTag.findFirstSubTag("type");
        if (typeSubTag != null) {
          pType = typeSubTag.getAttributeValue("type");
        }
      }
      String pOptional = paramTag.getAttributeValue("optional");
      if (pName != null && pCode != null && pType != null) {
        boolean bOptional = false;
        if ("yes".equals(pOptional)) {
          bOptional = true;
        }
        commandParameter = new CommandParameterImpl(command, pName, pCode, bOptional, pType, pDescription, paramTag);
      }
      if (commandParameter != null) {
        commandParameters.add(commandParameter);
      }
    }
    command.setDirectParameter(directParameter);
    command.setParameters(commandParameters);
//    result = new AppleScriptCommandImpl(suite, name, code, commandParameters, directParameter, null, description);
    return command;
  }
}
