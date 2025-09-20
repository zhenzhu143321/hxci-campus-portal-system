#!/usr/bin/env python3
"""
Gemini to OpenRouter Proxy Server
将Gemini API格式转换为OpenRouter格式的代理服务器
"""

import json
import logging
from flask import Flask, request, jsonify, Response
import requests
from typing import Dict, Any
import os

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)

# OpenRouter配置
OPENROUTER_API_KEY = os.getenv('OPENROUTER_API_KEY',
    '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')
OPENROUTER_BASE_URL = 'https://openrouter.ai/api/v1'
OPENROUTER_MODEL = os.getenv('OPENROUTER_MODEL', 'google/gemini-2.5-pro')

# 模型映射
MODEL_MAPPING = {
    'gemini-2.5-pro': 'google/gemini-2.5-pro',
    'gemini-2.5-flash': 'google/gemini-2.5-flash',
    'gemini-pro': 'google/gemini-2.5-pro',
    'gemini-flash': 'google/gemini-2.5-flash'
}

# 代理服务配置
PROXY_HOST = os.getenv('PROXY_HOST', '127.0.0.1')
PROXY_PORT = int(os.getenv('PROXY_PORT', '8888'))

def get_openrouter_model(gemini_model: str) -> str:
    """
    获取对应的OpenRouter模型名称
    """
    # 移除可能的前缀
    model_name = gemini_model.replace('models/', '').replace('gemini/', '')
    
    # 查找映射
    return MODEL_MAPPING.get(model_name, OPENROUTER_MODEL)

def convert_gemini_to_openrouter(gemini_request: Dict[str, Any], model: str = None) -> Dict[str, Any]:
    """
    将Gemini API请求格式转换为OpenRouter格式
    """
    messages = []
    
    # 处理Gemini的contents格式
    if 'contents' in gemini_request:
        for content in gemini_request['contents']:
            role = content.get('role', 'user')
            # 转换角色名称
            if role == 'model':
                role = 'assistant'
            
            # 提取文本内容
            text_parts = []
            if 'parts' in content:
                for part in content['parts']:
                    if 'text' in part:
                        text_parts.append(part['text'])
            
            if text_parts:
                messages.append({
                    'role': role,
                    'content': ' '.join(text_parts)
                })
    
    # 处理简单的prompt格式
    elif 'prompt' in gemini_request:
        messages.append({
            'role': 'user',
            'content': gemini_request['prompt']
        })
    
    # 构建OpenRouter请求
    openrouter_request = {
        'model': get_openrouter_model(model) if model else OPENROUTER_MODEL,
        'messages': messages,
        'max_tokens': gemini_request.get('maxOutputTokens', 8000),
        'temperature': gemini_request.get('temperature', 0.7),
        'stream': gemini_request.get('stream', False)
    }
    
    # 添加系统指令（如果有）
    if 'systemInstruction' in gemini_request:
        system_content = gemini_request['systemInstruction']
        if isinstance(system_content, dict) and 'parts' in system_content:
            system_text = ' '.join([p.get('text', '') for p in system_content['parts']])
        else:
            system_text = str(system_content)
        
        messages.insert(0, {
            'role': 'system',
            'content': system_text
        })
    
    return openrouter_request

def convert_openrouter_to_gemini(openrouter_response: Dict[str, Any]) -> Dict[str, Any]:
    """
    将OpenRouter响应格式转换为Gemini格式
    """
    candidates = []
    
    if 'choices' in openrouter_response:
        for choice in openrouter_response['choices']:
            message = choice.get('message', {})
            content = message.get('content', '')
            
            candidate = {
                'content': {
                    'parts': [{'text': content}],
                    'role': 'model'
                },
                'finishReason': choice.get('finish_reason', 'STOP'),
                'index': choice.get('index', 0)
            }
            
            # 添加安全评级（Gemini格式）
            candidate['safetyRatings'] = [
                {
                    'category': 'HARM_CATEGORY_SEXUALLY_EXPLICIT',
                    'probability': 'NEGLIGIBLE'
                },
                {
                    'category': 'HARM_CATEGORY_HATE_SPEECH',
                    'probability': 'NEGLIGIBLE'
                },
                {
                    'category': 'HARM_CATEGORY_HARASSMENT',
                    'probability': 'NEGLIGIBLE'
                },
                {
                    'category': 'HARM_CATEGORY_DANGEROUS_CONTENT',
                    'probability': 'NEGLIGIBLE'
                }
            ]
            
            candidates.append(candidate)
    
    # 构建Gemini响应格式
    gemini_response = {
        'candidates': candidates
    }
    
    # 添加使用统计（如果有）
    if 'usage' in openrouter_response:
        usage = openrouter_response['usage']
        gemini_response['usageMetadata'] = {
            'promptTokenCount': usage.get('prompt_tokens', 0),
            'candidatesTokenCount': usage.get('completion_tokens', 0),
            'totalTokenCount': usage.get('total_tokens', 0)
        }
    
    return gemini_response

@app.route('/v1beta/models/<model>:streamGenerateContent', methods=['POST'])
def stream_generate_content(model):
    """
    处理Gemini API的流式生成请求
    """
    try:
        logger.info(f"Received streaming request for model: {model}")
        
        # 获取Gemini请求数据
        gemini_request = request.get_json()
        
        # 转换为OpenRouter格式（启用流式模式）
        openrouter_request = convert_gemini_to_openrouter(gemini_request, model)
        openrouter_request['stream'] = True
        
        logger.info(f"Converted to OpenRouter format (streaming): {json.dumps(openrouter_request, ensure_ascii=False)[:500]}")
        
        # 调用OpenRouter API（流式）
        headers = {
            'Authorization': f'Bearer {OPENROUTER_API_KEY}',
            'Content-Type': 'application/json',
            'HTTP-Referer': 'https://github.com/hxci-campus-portal',
            'X-Title': 'Gemini CLI Proxy'
        }
        
        def generate():
            response = requests.post(
                f'{OPENROUTER_BASE_URL}/chat/completions',
                headers=headers,
                json=openrouter_request,
                stream=True,
                timeout=60
            )
            
            for line in response.iter_lines():
                if line:
                    line_str = line.decode('utf-8')
                    if line_str.startswith('data: '):
                        data_str = line_str[6:]
                        if data_str == '[DONE]':
                            # SSE结束信号
                            yield 'data: {"candidates": [{"finishReason": "STOP"}]}\n\n'
                            break
                        
                        try:
                            # 解析OpenRouter的流式响应
                            chunk = json.loads(data_str)
                            if 'choices' in chunk and chunk['choices']:
                                delta = chunk['choices'][0].get('delta', {})
                                content = delta.get('content', '')
                                
                                if content:
                                    # 转换为Gemini流式格式
                                    gemini_chunk = {
                                        'candidates': [{
                                            'content': {
                                                'parts': [{'text': content}],
                                                'role': 'model'
                                            }
                                        }]
                                    }
                                    yield f'data: {json.dumps(gemini_chunk)}\n\n'
                        except json.JSONDecodeError:
                            logger.error(f"Failed to parse streaming chunk: {data_str}")
        
        return Response(generate(), mimetype='text/event-stream')
        
    except Exception as e:
        logger.error(f"Error processing streaming request: {str(e)}", exc_info=True)
        return jsonify({
            'error': {
                'message': str(e),
                'code': 500
            }
        }), 500

@app.route('/v1beta/models/<model>:generateContent', methods=['POST'])
@app.route('/v1beta/models/<model>/generateContent', methods=['POST'])
@app.route('/v1/models/<model>:generateContent', methods=['POST'])
def generate_content(model):
    """
    处理Gemini API的generateContent请求
    """
    try:
        # 记录请求
        logger.info(f"Received request for model: {model}")
        logger.debug(f"Request headers: {dict(request.headers)}")
        logger.debug(f"Request body: {request.get_json()}")
        
        # 获取Gemini请求数据
        gemini_request = request.get_json()
        
        # 转换为OpenRouter格式
        openrouter_request = convert_gemini_to_openrouter(gemini_request, model)
        logger.info(f"Converted to OpenRouter format: {json.dumps(openrouter_request, ensure_ascii=False)[:500]}")
        
        # 调用OpenRouter API
        headers = {
            'Authorization': f'Bearer {OPENROUTER_API_KEY}',
            'Content-Type': 'application/json',
            'HTTP-Referer': 'https://github.com/hxci-campus-portal',
            'X-Title': 'Gemini CLI Proxy'
        }
        
        response = requests.post(
            f'{OPENROUTER_BASE_URL}/chat/completions',
            headers=headers,
            json=openrouter_request,
            timeout=60
        )
        
        logger.info(f"OpenRouter response status: {response.status_code}")
        
        if response.status_code != 200:
            logger.error(f"OpenRouter error: {response.text}")
            return jsonify({
                'error': {
                    'message': f'OpenRouter API error: {response.text}',
                    'code': response.status_code
                }
            }), response.status_code
        
        # 转换响应格式
        openrouter_response = response.json()
        gemini_response = convert_openrouter_to_gemini(openrouter_response)
        
        logger.info("Successfully converted response to Gemini format")
        return jsonify(gemini_response)
        
    except Exception as e:
        logger.error(f"Error processing request: {str(e)}", exc_info=True)
        return jsonify({
            'error': {
                'message': str(e),
                'code': 500
            }
        }), 500

@app.route('/v1beta/models', methods=['GET'])
@app.route('/v1/models', methods=['GET'])
def list_models():
    """
    返回可用的模型列表
    """
    return jsonify({
        'models': [
            {
                'name': 'models/gemini-2.5-pro',
                'version': '001',
                'displayName': 'Gemini 2.5 Pro (via OpenRouter)',
                'description': 'Google Gemini 2.5 Pro through OpenRouter proxy'
            },
            {
                'name': 'models/gemini-2.5-flash',
                'version': '001',
                'displayName': 'Gemini 2.5 Flash (via OpenRouter)',
                'description': 'Google Gemini 2.5 Flash through OpenRouter proxy'
            }
        ]
    })

@app.route('/v1beta/models/<model>:countTokens', methods=['POST'])
@app.route('/v1/models/<model>:countTokens', methods=['POST'])
def count_tokens(model):
    """
    处理Gemini API的countTokens请求
    """
    try:
        gemini_request = request.get_json()
        
        # 提取文本内容
        text_content = ""
        if 'contents' in gemini_request:
            for content in gemini_request['contents']:
                if 'parts' in content:
                    for part in content['parts']:
                        if 'text' in part:
                            text_content += part['text'] + " "
        elif 'prompt' in gemini_request:
            text_content = gemini_request['prompt']
        
        # 简单的token估算（实际应该使用tiktoken）
        # 这里使用简单的估算：平均每4个字符为1个token
        estimated_tokens = len(text_content) // 4
        
        return jsonify({
            'totalTokens': estimated_tokens
        })
        
    except Exception as e:
        logger.error(f"Error counting tokens: {str(e)}")
        return jsonify({
            'error': {
                'message': str(e),
                'code': 500
            }
        }), 500

@app.route('/v1beta/models/<model>:generateJson', methods=['POST'])
@app.route('/v1/models/<model>:generateJson', methods=['POST'])
@app.route('/v1beta/models/<model>/generateJson', methods=['POST'])
@app.route('/v1/models/<model>/generateJson', methods=['POST'])
def generate_json(model):
    """
    处理Gemini CLI的generateJson请求
    根据CodeX分析，CLI期望真正的模型响应，而不仅仅是nextSpeaker决定
    """
    try:
        logger.info(f"Received generateJson request for model: {model}")
        
        # 获取请求数据
        gemini_request = request.get_json()
        logger.debug(f"GenerateJson request: {json.dumps(gemini_request, ensure_ascii=False)[:300]}")
        
        # 调用真正的模型生成JSON响应
        # 转换为OpenRouter格式
        openrouter_request = convert_gemini_to_openrouter(gemini_request, model)
        logger.info(f"Converted generateJson to OpenRouter format: {json.dumps(openrouter_request, ensure_ascii=False)[:500]}")
        
        # 调用OpenRouter API
        headers = {
            'Authorization': f'Bearer {OPENROUTER_API_KEY}',
            'Content-Type': 'application/json',
            'HTTP-Referer': 'https://github.com/hxci-campus-portal',
            'X-Title': 'Gemini CLI Proxy'
        }
        
        response = requests.post(
            f'{OPENROUTER_BASE_URL}/chat/completions',
            headers=headers,
            json=openrouter_request,
            timeout=60
        )
        
        logger.info(f"OpenRouter generateJson response status: {response.status_code}")
        
        if response.status_code != 200:
            logger.error(f"OpenRouter generateJson error: {response.text}")
            # 返回默认的nextSpeaker响应
            return jsonify({
                "nextSpeaker": "user"
            })
        
        # 转换响应格式并提取nextSpeaker决定
        openrouter_response = response.json()
        
        # 从OpenRouter响应中提取文本
        next_speaker = "user"  # 默认值
        if 'choices' in openrouter_response and len(openrouter_response['choices']) > 0:
            choice = openrouter_response['choices'][0]
            if 'message' in choice and 'content' in choice['message']:
                content = choice['message']['content'].strip().lower()
                
                # 根据模型响应内容决定下一个发言者
                if any(keyword in content for keyword in ['user', '用户', 'user should', 'should be user']):
                    next_speaker = "user"
                elif any(keyword in content for keyword in ['model', '模型', 'assistant', 'model should', 'should be model']):
                    next_speaker = "model"
                else:
                    # 如果无法判断，根据上下文决定
                    # 如果模型刚刚回应，通常下一个应该是user
                    next_speaker = "user"
        
        # 返回CLI期望的格式
        response_data = {
            "nextSpeaker": next_speaker
        }
        
        logger.info(f"GenerateJson extracted nextSpeaker decision: {response_data}")
        return jsonify(response_data)
        
    except Exception as e:
        logger.error(f"Error in generateJson endpoint: {str(e)}", exc_info=True)
        # 返回默认nextSpeaker响应避免CLI报错
        return jsonify({
            "nextSpeaker": "user"
        })

@app.route('/health', methods=['GET'])
def health_check():
    """
    健康检查端点
    """
    return jsonify({
        'status': 'healthy',
        'service': 'Gemini to OpenRouter Proxy',
        'openrouter_model': OPENROUTER_MODEL
    })

@app.before_request
def log_request_info():
    """
    记录所有请求信息以帮助调试
    """
    if request.path not in ['/health']:
        logger.info(f"Incoming request: {request.method} {request.path}")
        logger.debug(f"Headers: {dict(request.headers)}")
        if request.is_json and request.get_json():
            logger.debug(f"Body: {str(request.get_json())[:200]}")

@app.errorhandler(404)
def not_found(error):
    """
    处理404错误，返回详细信息
    """
    logger.error(f"404 Not Found: {request.method} {request.path}")
    return jsonify({
        'error': {
            'message': f'Endpoint not found: {request.method} {request.path}',
            'code': 404
        }
    }), 404

if __name__ == '__main__':
    logger.info(f"Starting Gemini to OpenRouter Proxy on {PROXY_HOST}:{PROXY_PORT}")
    logger.info(f"Using OpenRouter model: {OPENROUTER_MODEL}")
    logger.info("To use this proxy with Gemini CLI:")
    logger.info(f"  export GOOGLE_GEMINI_BASE_URL='http://{PROXY_HOST}:{PROXY_PORT}'")
    logger.info(f"  export GEMINI_API_KEY='your-key-or-dummy'")
    
    app.run(host=PROXY_HOST, port=PROXY_PORT, debug=True)