/**
 * 前端性能测试脚本
 * 用于在浏览器控制台中运行，测量页面加载时间
 * 
 * 使用方法：
 * 1. 打开浏览器开发者工具（F12）
 * 2. 切换到 Console 标签
 * 3. 复制粘贴此脚本并执行
 */

(function() {
  'use strict';

  console.log('====================================');
  console.log('   前端性能测试工具');
  console.log('====================================');

  // 获取 Navigation Timing API 数据
  const timing = performance.timing;
  const navigation = performance.getEntriesByType('navigation')[0];

  // 计算关键性能指标
  const metrics = {
    // DNS 查询时间
    dnsLookup: timing.domainLookupEnd - timing.domainLookupStart,
    
    // TCP 连接时间
    tcpConnection: timing.connectEnd - timing.connectStart,
    
    // SSL 握手时间（如果是 HTTPS）
    sslHandshake: timing.secureConnectionStart > 0 
      ? timing.connectEnd - timing.secureConnectionStart 
      : 0,
    
    // 首字节时间 (TTFB)
    ttfb: timing.responseStart - timing.requestStart,
    
    // 响应下载时间
    responseTime: timing.responseEnd - timing.responseStart,
    
    // DOM 解析时间
    domParsing: timing.domInteractive - timing.responseEnd,
    
    // DOM 内容加载时间
    domContentLoaded: timing.domContentLoadedEventEnd - timing.navigationStart,
    
    // 页面完全加载时间
    pageLoad: timing.loadEventEnd - timing.navigationStart,
    
    // 首次绘制时间 (FP)
    firstPaint: 0,
    
    // 首次内容绘制时间 (FCP)
    firstContentfulPaint: 0,
    
    // 最大内容绘制时间 (LCP)
    largestContentfulPaint: 0
  };

  // 获取 Paint Timing
  const paintEntries = performance.getEntriesByType('paint');
  paintEntries.forEach(entry => {
    if (entry.name === 'first-paint') {
      metrics.firstPaint = Math.round(entry.startTime);
    }
    if (entry.name === 'first-contentful-paint') {
      metrics.firstContentfulPaint = Math.round(entry.startTime);
    }
  });

  // 获取 LCP（如果支持）
  if (window.PerformanceObserver) {
    try {
      const lcpEntries = performance.getEntriesByType('largest-contentful-paint');
      if (lcpEntries.length > 0) {
        metrics.largestContentfulPaint = Math.round(lcpEntries[lcpEntries.length - 1].startTime);
      }
    } catch (e) {
      // LCP 可能不被支持
    }
  }

  // 性能指标阈值
  const thresholds = {
    pageLoad: 2000,           // 页面加载时间 < 2秒
    ttfb: 500,                // 首字节时间 < 500ms
    firstContentfulPaint: 1800, // FCP < 1.8秒
    largestContentfulPaint: 2500 // LCP < 2.5秒
  };

  // 检查是否达标
  function checkThreshold(value, threshold) {
    return value <= threshold;
  }

  // 格式化输出
  function formatMs(ms) {
    return ms + 'ms';
  }

  function getStatus(passed) {
    return passed ? '✅ 达标' : '❌ 未达标';
  }

  // 输出结果
  console.log('\n📊 性能测试结果\n');
  
  console.log('=== 网络性能 ===');
  console.log(`DNS 查询时间:     ${formatMs(metrics.dnsLookup)}`);
  console.log(`TCP 连接时间:     ${formatMs(metrics.tcpConnection)}`);
  console.log(`SSL 握手时间:     ${formatMs(metrics.sslHandshake)}`);
  console.log(`首字节时间(TTFB): ${formatMs(metrics.ttfb)} ${getStatus(checkThreshold(metrics.ttfb, thresholds.ttfb))}`);
  console.log(`响应下载时间:     ${formatMs(metrics.responseTime)}`);

  console.log('\n=== 渲染性能 ===');
  console.log(`DOM 解析时间:     ${formatMs(metrics.domParsing)}`);
  console.log(`DOM 内容加载:     ${formatMs(metrics.domContentLoaded)}`);
  console.log(`首次绘制(FP):     ${formatMs(metrics.firstPaint)}`);
  console.log(`首次内容绘制(FCP):${formatMs(metrics.firstContentfulPaint)} ${getStatus(checkThreshold(metrics.firstContentfulPaint, thresholds.firstContentfulPaint))}`);

  console.log('\n=== 关键指标 ===');
  const pageLoadPassed = checkThreshold(metrics.pageLoad, thresholds.pageLoad);
  console.log(`页面加载时间:     ${formatMs(metrics.pageLoad)} ${getStatus(pageLoadPassed)}`);
  console.log(`目标: < ${formatMs(thresholds.pageLoad)}`);

  // 资源加载统计
  console.log('\n=== 资源加载统计 ===');
  const resources = performance.getEntriesByType('resource');
  const resourceStats = {
    js: { count: 0, size: 0, time: 0 },
    css: { count: 0, size: 0, time: 0 },
    img: { count: 0, size: 0, time: 0 },
    font: { count: 0, size: 0, time: 0 },
    other: { count: 0, size: 0, time: 0 }
  };

  resources.forEach(resource => {
    const type = getResourceType(resource.name);
    resourceStats[type].count++;
    resourceStats[type].size += resource.transferSize || 0;
    resourceStats[type].time += resource.duration;
  });

  function getResourceType(url) {
    if (url.match(/\.(js|mjs)(\?|$)/i)) return 'js';
    if (url.match(/\.css(\?|$)/i)) return 'css';
    if (url.match(/\.(png|jpg|jpeg|gif|webp|svg|ico)(\?|$)/i)) return 'img';
    if (url.match(/\.(woff|woff2|ttf|eot)(\?|$)/i)) return 'font';
    return 'other';
  }

  function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  }

  console.log(`JavaScript: ${resourceStats.js.count} 个文件, ${formatBytes(resourceStats.js.size)}, ${Math.round(resourceStats.js.time)}ms`);
  console.log(`CSS:        ${resourceStats.css.count} 个文件, ${formatBytes(resourceStats.css.size)}, ${Math.round(resourceStats.css.time)}ms`);
  console.log(`图片:       ${resourceStats.img.count} 个文件, ${formatBytes(resourceStats.img.size)}, ${Math.round(resourceStats.img.time)}ms`);
  console.log(`字体:       ${resourceStats.font.count} 个文件, ${formatBytes(resourceStats.font.size)}, ${Math.round(resourceStats.font.time)}ms`);
  console.log(`其他:       ${resourceStats.other.count} 个文件, ${formatBytes(resourceStats.other.size)}, ${Math.round(resourceStats.other.time)}ms`);

  const totalSize = resources.reduce((sum, r) => sum + (r.transferSize || 0), 0);
  console.log(`\n总计: ${resources.length} 个资源, ${formatBytes(totalSize)}`);

  // 最终结论
  console.log('\n====================================');
  console.log('   测试结论');
  console.log('====================================');
  
  if (pageLoadPassed) {
    console.log('✅ 页面加载时间符合要求 (< 2秒)');
  } else {
    console.log('❌ 页面加载时间超出要求 (> 2秒)');
    console.log('   建议优化措施：');
    console.log('   1. 启用 Gzip 压缩');
    console.log('   2. 使用 CDN 加速');
    console.log('   3. 优化图片大小');
    console.log('   4. 延迟加载非关键资源');
  }

  // 返回测试数据供导出
  return {
    metrics,
    thresholds,
    resourceStats,
    totalResources: resources.length,
    totalSize,
    passed: pageLoadPassed,
    timestamp: new Date().toISOString()
  };
})();
