package com.company.XMLSerializer;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class XmlSerializer {
  public static void serialize(Object object, String filePath) throws IOException, IllegalClassFormatException, IllegalAccessException,  java.lang.reflect.InvocationTargetException
  {
    FileWriter writer = new FileWriter(filePath);
    convertToDocument(object).write(writer);
    writer.close();
  }

  public static Document convertToDocument(Object obj) throws IllegalClassFormatException, IllegalAccessException,  java.lang.reflect.InvocationTargetException
  {
    Class<? extends Object> clazz = obj.getClass();
    Document document = DocumentHelper.createDocument();
    Element root = document.addElement(clazz.getSimpleName());
    Map<String,Element> tags = new HashMap<>();

    if (!clazz.isAnnotationPresent(XmlObject.class))
    {
      throw new IllegalClassFormatException(clazz.getName() + " doesn't have XmlObject annotation");
    }
    Field[] fields = clazz.getDeclaredFields();
    for(Field field : fields)
    {

      field.setAccessible(true);
      if(field.isAnnotationPresent(XmlTag.class))
      {
        String name = field.getAnnotation(XmlTag.class).name();
        name=(name.equals("") ? field.getName() : name);
        if(tags.containsKey(name))
        {
          throw new IllegalClassFormatException("Two or more tags with name "+name);
        }
        Element element = root.addElement(name);
        tags.put(name, element);
        if(field.getType().isAnnotationPresent(XmlObject.class))
        {
          element.add(convertToDocument(field.get(obj)).getRootElement());
        }
        else
        {
          element.addText(field.get(obj).toString());
        }
      }
    }
    for(Field field : fields)
    {
      field.setAccessible(true);
      if(field.isAnnotationPresent(XmlAttribute.class))
      {
        String name = field.getAnnotation(XmlAttribute.class).name();
        name = (name.equals("") ? field.getName():name);
        String tagName = field.getAnnotation(XmlAttribute.class).tag();
        Element tag = (tagName.equals("")? root : tags.get(tagName));
        var list = tag.attributes();
        for(var attribute : list)
        {
          if(attribute.getName().equals(tagName))
          {
            throw  new IllegalClassFormatException("Two or more attributes with name " + name);
          }
        }
        tag.addAttribute(name, field.get(obj).toString());
      }
    }
    Method[] methods = clazz.getDeclaredMethods();
    for(Method method : methods)
    {
      method.setAccessible(true);
      if(method.isAnnotationPresent(XmlTag.class))
      {
        if(method.getParameterCount()!=0 || method.getReturnType().equals(void.class))
        {
          throw new IllegalClassFormatException(method.toString()+" serialization error");
        }

        String name = method.getAnnotation(XmlTag.class).name();
        name=(name.equals("") ? method.getName() : name);
        if(name.equals(method.getName()))
        {
          if(name.startsWith("get"))
          {
            name = name.substring(3);
          }
        }

        Element element = root.addElement(name);
        if(method.getReturnType().isAnnotationPresent(XmlObject.class))
        {
          element.add(convertToDocument(method.invoke(obj)).getRootElement());
        }
        else
        {
          element.addText(method.invoke(obj).toString());
        }
        if(!tags.containsKey(name))
        {
          tags.put(name,element);
        }
        else
        {
          throw new IllegalClassFormatException("Two or more tags with name "+name);
        }

      }

    }
    for(Method method : methods)
    {
      method.setAccessible(true);
      if(method.isAnnotationPresent(XmlAttribute.class))
      {
        if(method.getParameterCount()!=0 || method.getReturnType().equals(void.class))
        {
          throw new IllegalClassFormatException(method.toString()+" serialization error");
        }
        String name = method.getAnnotation(XmlAttribute.class).name();
        name=(name.equals("") ? method.getName() : name);
        if(name.equals(method.getName()))
        {
          if(name.startsWith("get"))
          {
            name = name.substring(3);
          }
        }
        String tagName = method.getAnnotation(XmlAttribute.class).tag();
        Element tag = (tagName.equals("")? root : tags.get(tagName));
        var list = tag.attributes();
        for(var attribute : list)
        {
          if(attribute.getName().equals(name))
          {
            throw  new IllegalClassFormatException("Two or more attributes with name " + name);
          }
        }
        tag.addAttribute(name, method.invoke(obj).toString());
      }
    }

    return document;
  }
}
