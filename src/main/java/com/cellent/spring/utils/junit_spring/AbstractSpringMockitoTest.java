package com.cellent.spring.utils.junit_spring;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mockito.Mockito;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Class which contains a kind of application context, to make autowiring work
 * in tests without having to build up a real application context. It vontains a
 * map of Objects which may be injected as Objects and another Map which
 * contains instances which will be injected on {@link Value} annotations.
 * 
 * @author bjoern
 */
public final class AbstractSpringMockitoTest implements BeanInstanceProvider {

	/**
	 * A Map with all {@link Value}s.
	 */
	private Map<String, Object> atValueMap;

	/**
	 * Cache for beans in the context. Will be used by
	 * {@link MockitoTestBeanFactory#resolveDependency(DependencyDescriptor, String, Set, TypeConverter)}
	 */
	@SuppressWarnings("rawtypes")
	private Map<Class, Object> mockInstanceMap;

	/**
	 * @see AutowiredAnnotationBeanPostProcessor. This will be effectively
	 *      important to call
	 *      {@link MockitoTestBeanFactory#resolveDependency(DependencyDescriptor, String, Set, TypeConverter)}
	 *      .
	 */
	private AutowiredAnnotationBeanPostProcessor autowirePostProcessor;

	/**
	 * To instantiate by Spring and do constructor injection. Will be using
	 * {@link MockitoTestBeanFactory#getBean(Class)}.
	 */
	ApplicationContext applicationContext;

	/**
	 * Create an object (you might call it context or factory as well) which
	 * allows to do spring autowiring also in test classes without any special
	 * test runner. Eventually, instantiate the class under Test by
	 * {@link #createBean(Class)}.
	 */
	@SuppressWarnings("rawtypes")
	public AbstractSpringMockitoTest() {
		// Initialisiere den Object Cache ({@link #mockInstanceMap},
		// Pseudo-ApplicationContext) und den {@link #autowirePostProcessor}.
		mockInstanceMap = new HashMap<Class, Object>();
		atValueMap = new HashMap<String, Object>();
		// Spring Infrastruktur
		autowirePostProcessor = new AutowiredAnnotationBeanPostProcessor();
		applicationContext = new GenericApplicationContext(
				new MockitoTestBeanFactory(this));
		autowirePostProcessor.setBeanFactory(applicationContext
				.getAutowireCapableBeanFactory());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cellent.spring.utils.junit_spring.BeanInstanceProvider#createBean
	 * (java.lang.Class)
	 */
	public <T> T createBean(Class<T> desiredClass) {
		T result = applicationContext.getBean(desiredClass);
		// Führe Field Injection aus.
		autowirePostProcessor.processInjection(result);
		if (result instanceof InitializingBean) {
			try {
				((InitializingBean) result).afterPropertiesSet();
			} catch (Exception e) {
				throw new RuntimeException(
						"Class is InitializingBean, but calling afterPropertiesSet leads to an error: "
								+ e.getMessage(), e);
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cellent.spring.utils.junit_spring.BeanInstanceProvider#registerInstance
	 * (java.lang.Object)
	 */
	public void registerInstance(Object beanInstance) {
		mockInstanceMap.put(beanInstance.getClass(), beanInstance);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cellent.spring.utils.junit_spring.BeanInstanceProvider#getInstanceOf
	 * (java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T> T getInstanceOf(Class<T> clazz) {
		if (mockInstanceMap.containsKey(clazz) || discoverInstanceOf(clazz))
			return (T) mockInstanceMap.get(clazz);
		else {
			T result = createMockInstance(clazz);
			registerInstance(result);
			return result;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cellent.spring.utils.junit_spring.BeanInstanceProvider#setValue(java
	 * .lang.String, java.lang.Object)
	 */
	public void setValue(String key, Object value) {
		atValueMap.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cellent.spring.utils.junit_spring.BeanInstanceProvider#getValue(java
	 * .lang.String)
	 */
	public Object getValue(String value) {
		return atValueMap.get(value);
	}

	/**
	 * Search fo a class in a set of known instances. If one is found, a pair of
	 * class/object will be cached in {@link #mockInstanceMap} and true is
	 * returned, otherwise false.
	 * 
	 * If true is returned, you can obtain your instance via
	 * {@link Map#get(Object)} on {@link #mockInstanceMap}.
	 * 
	 * @param clazz
	 *            The class you are looking for.
	 * @return true, if {@link #mockInstanceMap} holds an instance of this
	 *         class, false otherwise.
	 */
	private <T> boolean discoverInstanceOf(Class<T> clazz) {
		Collection<Object> instaces = mockInstanceMap.values();
		for (Object object : instaces) {
			if (clazz.isInstance(object)) {
				mockInstanceMap.put(clazz, object);
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a mocked instance of the desired class.
	 * 
	 * @param requiredType
	 *            The desired class.
	 * @return A {@link Mockito}-Mock of the desired class.
	 */
	private <T> T createMockInstance(Class<T> requiredType) {
		return Mockito.mock(requiredType);
	}

}