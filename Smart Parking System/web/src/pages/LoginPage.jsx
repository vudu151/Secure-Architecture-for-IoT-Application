import React from 'react';
import { Form, Input, Button, Card, Typography, Space, message } from 'antd';
import { UserOutlined, LockOutlined, CarOutlined } from '@ant-design/icons';
import { useNavigate, Navigate } from 'react-router-dom';
import useAuthStore from '../stores/authStore';

const { Title, Text } = Typography;

const LoginPage = () => {
  const { login, isAuthenticated, user, loading } = useAuthStore();
  const navigate = useNavigate();

  // Redirect already-authenticated users to their own portal
  if (isAuthenticated) {
    if (user?.role === 'ADMIN') return <Navigate to="/dashboard" replace />;
    if (user?.role === 'DRIVER') return <Navigate to="/driver" replace />;
  }

  const onFinish = async (values) => {
    const result = await login(values.email, values.password);
    if (result?.success) {
      if (result.role === 'ADMIN') {
        navigate('/dashboard');
      } else if (result.role === 'DRIVER') {
        navigate('/driver');
      }
    }
  };

  return (
    <div className="login-page-container">
      <Card
        className="glass-card"
        style={{
          width: 400,
          border: 'none',
          padding: '24px 16px',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Space direction="vertical" size="small">
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: '50%',
                background: 'rgba(26, 35, 126, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 12px',
              }}
            >
              <CarOutlined style={{ fontSize: 32, color: '#1a237e' }} />
            </div>
            <Title level={3} style={{ margin: 0, color: '#1a237e', fontWeight: 'bold' }}>
              SPARK ADMIN
            </Title>
            <Text type="secondary">Đăng nhập cổng quản trị hệ thống</Text>
          </Space>
        </div>

        <Form
          name="login_form"
          initialValues={{ remember: true }}
          onFinish={onFinish}
          size="large"
          layout="vertical"
        >
          <Form.Item
            name="email"
            rules={[
              { required: true, message: 'Vui lòng nhập email!' },
              { type: 'email', message: 'Email không đúng định dạng!' },
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
              placeholder="Email quản trị"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu!' }]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
              placeholder="Mật khẩu"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 8 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              style={{
                width: '100%',
                height: 48,
                background: 'linear-gradient(135deg, #1a237e 0%, #0d47a1 100%)',
                border: 'none',
                boxShadow: '0 4px 12px rgba(26, 35, 126, 0.3)',
                fontWeight: 'bold',
              }}
            >
              Đăng nhập
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default LoginPage;
