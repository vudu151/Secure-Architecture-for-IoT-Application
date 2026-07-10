import React, { useState } from 'react';
import {
  Layout, Menu, Button, Avatar, Dropdown, Space, Typography, Badge,
} from 'antd';
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  CalendarOutlined,
  CarOutlined,
  WalletOutlined,
  HistoryOutlined,
  LogoutOutlined,
  UserOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import useAuthStore from '../stores/authStore';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const DriverLayout = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const menuItems = [
    {
      key: '/driver',
      icon: <HomeOutlined />,
      label: 'Tổng quan',
    },
    {
      key: '/driver/parking',
      icon: <CarOutlined />,
      label: 'Đặt chỗ đỗ xe',
    },
    {
      key: '/driver/bookings',
      icon: <CalendarOutlined />,
      label: 'Lịch sử đặt chỗ',
    },
    {
      key: '/driver/wallet',
      icon: <WalletOutlined />,
      label: 'Ví tiền',
    },
    {
      key: '/driver/vehicles',
      icon: <HistoryOutlined />,
      label: 'Xe của tôi',
    },
  ];

  const userDropdownItems = {
    items: [
      {
        key: 'name',
        label: <Text strong>{user?.fullName || 'Tài xế'}</Text>,
        disabled: true,
      },
      {
        key: 'email',
        label: <Text type="secondary">{user?.email}</Text>,
        disabled: true,
      },
      { type: 'divider' },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: 'Đăng xuất',
        onClick: handleLogout,
        danger: true,
      },
    ],
  };

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  const currentMenuItem = menuItems.find((item) =>
    location.pathname === item.key || location.pathname.startsWith(item.key + '/')
  );
  const pageTitle = currentMenuItem ? currentMenuItem.label : 'Smart Parking';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="dark"
        width={240}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          background: 'linear-gradient(180deg, #1a237e 0%, #0d47a1 100%)',
        }}
      >
        {/* Logo */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding: collapsed ? '0' : '0 24px',
            borderBottom: '1px solid rgba(255,255,255,0.1)',
          }}
        >
          <Space size="middle">
            <CarOutlined style={{ fontSize: 24, color: '#64b5f6' }} />
            {!collapsed && (
              <span
                style={{
                  color: '#fff',
                  fontSize: 16,
                  fontWeight: 'bold',
                  letterSpacing: '1px',
                  whiteSpace: 'nowrap',
                }}
              >
                SPARK DRIVER
              </span>
            )}
          </Space>
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[
            menuItems.find(
              (item) =>
                location.pathname === item.key ||
                location.pathname.startsWith(item.key + '/')
            )?.key || '/driver',
          ]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{
            padding: '16px 0',
            background: 'transparent',
          }}
        />
      </Sider>

      <Layout style={{ marginLeft: collapsed ? 80 : 240, transition: 'all 0.2s' }}>
        <Header
          style={{
            padding: '0 24px',
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
            position: 'sticky',
            top: 0,
            zIndex: 100,
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{ fontSize: '16px', width: 64, height: 64 }}
          />

          <span style={{ fontSize: 18, fontWeight: 'bold', color: '#1a237e', flex: 1, marginLeft: 8 }}>
            {pageTitle}
          </span>

          <Space size="large">
            <Dropdown menu={userDropdownItems} placement="bottomRight" trigger={['click']}>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar
                  style={{
                    background: 'linear-gradient(135deg, #1a237e 0%, #0d47a1 100%)',
                  }}
                  icon={<UserOutlined />}
                />
                {!collapsed && (
                  <span style={{ color: '#595959', fontWeight: 500 }}>
                    {user?.fullName || 'Tài xế'}
                  </span>
                )}
              </Space>
            </Dropdown>
          </Space>
        </Header>

        <Content
          style={{
            margin: '24px',
            padding: 0,
            minHeight: 280,
          }}
        >
          <Outlet />
        </Content>

        <div style={{ textAlign: 'center', padding: '24px 0', color: '#8c8c8c' }}>
          Smart Parking System ©2026. Secure IoT Architecture Project (HUST).
        </div>
      </Layout>
    </Layout>
  );
};

export default DriverLayout;
