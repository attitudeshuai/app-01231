/**
 * 统一表单验证规则模块
 *
 * 提供常用的表单验证规则和自定义验证器，配合 Element Plus 的 el-form 使用。
 * 所有验证规则返回 FormItemRule 或 FormItemRule[] 格式。
 *
 * @module utils/validators
 */

/** 验证规则类型 */
interface ValidatorRule {
  required?: boolean
  message?: string
  trigger?: string | string[]
  min?: number
  max?: number
  type?: string
  pattern?: RegExp
  validator?: (rule: any, value: any, callback: (error?: Error) => void) => void
}

/**
 * 必填验证规则
 *
 * @param message - 验证失败提示信息
 * @param trigger - 触发方式，默认 'blur'
 */
export const required = (message: string, trigger: string | string[] = 'blur'): ValidatorRule => ({
  required: true,
  message,
  trigger
})

/**
 * 长度范围验证
 *
 * @param min - 最小长度
 * @param max - 最大长度
 * @param message - 验证失败提示信息（可选，自动生成）
 */
export const lengthRange = (min: number, max: number, message?: string): ValidatorRule => ({
  min,
  max,
  message: message || `长度在 ${min} 到 ${max} 个字符之间`,
  trigger: 'blur'
})

/**
 * 手机号验证器
 */
export const phoneValidator: ValidatorRule = {
  pattern: /^1[3-9]\d{9}$/,
  message: '请输入正确的手机号',
  trigger: 'blur'
}

/**
 * 邮箱验证器
 */
export const emailValidator: ValidatorRule = {
  type: 'email',
  message: '请输入正确的邮箱地址',
  trigger: 'blur'
}

/**
 * ISBN验证器（支持ISBN-10和ISBN-13）
 */
export const isbnValidator: ValidatorRule = {
  pattern: /^(?:ISBN(?:-1[03])?:?\s?)?(?=[-0-9 ]{17}$|[-0-9X ]{13}$|[0-9X]{10}$|[0-9]{13}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?(?:[0-9]+[- ]?){2}[0-9X]$/i,
  message: '请输入正确的ISBN号',
  trigger: 'blur'
}

/**
 * 密码强度验证器（至少6位）
 */
export const passwordValidator: ValidatorRule = {
  validator: (_rule: any, value: any, callback: (error?: Error) => void) => {
    if (!value) {
      callback(new Error('请输入密码'))
    } else if (value.length < 6) {
      callback(new Error('密码长度不少于6位'))
    } else {
      callback()
    }
  },
  trigger: 'blur'
}

/**
 * 确认密码验证器工厂
 *
 * @param getPassword - 获取原始密码值的函数
 */
export const confirmPasswordValidator = (
  getPassword: () => string
): ValidatorRule => ({
  validator: (_rule: any, value: any, callback: (error?: Error) => void) => {
    if (!value) {
      callback(new Error('请再次输入密码'))
    } else if (value !== getPassword()) {
      callback(new Error('两次输入密码不一致'))
    } else {
      callback()
    }
  },
  trigger: 'blur'
})

/**
 * 正整数验证器
 */
export const positiveIntValidator: ValidatorRule = {
  validator: (_rule: any, value: any, callback: (error?: Error) => void) => {
    if (value === undefined || value === null || value === '') {
      callback()
      return
    }
    const num = Number(value)
    if (!Number.isInteger(num) || num <= 0) {
      callback(new Error('请输入正整数'))
    } else {
      callback()
    }
  },
  trigger: 'blur'
}

/**
 * 价格验证器（大于等于0，最多两位小数）
 */
export const priceValidator: ValidatorRule = {
  validator: (_rule: any, value: any, callback: (error?: Error) => void) => {
    if (value === undefined || value === null || value === '') {
      callback()
      return
    }
    const num = Number(value)
    if (isNaN(num) || num < 0) {
      callback(new Error('请输入有效的价格'))
    } else if (!/^\d+(\.\d{1,2})?$/.test(String(value))) {
      callback(new Error('价格最多两位小数'))
    } else {
      callback()
    }
  },
  trigger: 'blur'
}

/**
 * 用户名验证器（2-20位字母数字下划线）
 */
export const usernameValidator: ValidatorRule = {
  pattern: /^[a-zA-Z0-9_]{2,20}$/,
  message: '用户名由2-20位字母、数字或下划线组成',
  trigger: 'blur'
}

/**
 * 预定义的通用表单规则集合
 */
export const formRules = {
  /** 用户名规则 */
  username: [
    required('请输入用户名'),
    usernameValidator
  ],

  /** 密码规则 */
  password: [
    required('请输入密码'),
    passwordValidator
  ],

  /** 邮箱规则 */
  email: [
    required('请输入邮箱'),
    emailValidator
  ],

  /** 手机号规则 */
  phone: [
    phoneValidator
  ],

  /** 图书标题规则 */
  bookTitle: [
    required('请输入书名'),
    lengthRange(1, 200, '书名长度在1到200个字符之间')
  ],

  /** 图书作者规则 */
  bookAuthor: [
    required('请输入作者'),
    lengthRange(1, 100, '作者长度在1到100个字符之间')
  ],

  /** ISBN规则 */
  isbn: [
    required('请输入ISBN')
  ],

  /** 价格规则 */
  price: [
    priceValidator
  ],

  /** 库存规则 */
  stock: [
    positiveIntValidator
  ]
}
