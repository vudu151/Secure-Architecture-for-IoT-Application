import React, { useEffect, useState } from 'react';
import { Card, Table, Tag, Input, Button, Popconfirm, Badge, Space, Row, Col, message } from 'antd';
import { SearchOutlined, PoweroffOutlined } from '@ant-design/icons';
import { adminApi } from '../api/adminApi';
import { formatVND, formatDateTime } from '../constants';

const UsersPage = () => {
  const [loading, setLoading] = useState(true);
  const [users, setUsers] = useState([]);
  const [searchText, setSearchText] = useState('');

  const loadUsers = async () => {
    setLoading(true);
    try {
      const { data } = await adminApi.getUsers();
      const userList = data.data || data;
      setUsers(Array.isArray(userList) ? userList : []);
      setLoading(false);
    } catch (error) {
      setLoading(false);
      message.error('Không thể tải danh sách tài xế');
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleToggleActive = async (id) => {
    try {
      await adminApi.toggleUserActive(id);
      message.success('Cập nhật trạng thái tài xế thành công');
      loadUsers(); // reload lists
    } catch (error) {
      message.error(`Lỗi cập nhật trạng thái: ${error.message}`);
    }
  };

  // Filter users based on search query
  const filteredUsers = users.filter((u) => {
    const text = searchText.toLowerCase();
    return (
      u.fullName.toLowerCase().includes(text) ||
      u.email.toLowerCase().includes(text) ||
      (u.phone && u.phone.includes(text))
    );
  });

  const columns = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (text) => <strong>{text}</strong>,
      sorter: (a, b) => a.fullName.localeCompare(b.fullName),
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Số điện thoại',
      dataIndex: 'phone',
      key: 'phone',
      render: (text) => text || '—',
    },
    {
      title: 'Vai trò',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <Tag color={role === 'ADMIN' ? 'red' : 'blue'}>
          {role === 'ADMIN' ? 'Quản trị' : 'Tài xế'}
        </Tag>
      ),
    },
    {
      title: 'Số dư ví',
      dataIndex: 'balance',
      key: 'balance',
      render: (balance) => formatVND(balance),
      sorter: (a, b) => a.balance - b.balance,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (isActive) => (
        <Badge
          status={isActive ? 'success' : 'error'}
          text={isActive ? 'Hoạt động' : 'Bị khóa'}
        />
      ),
      filters: [
        { text: 'Hoạt động', value: true },
        { text: 'Bị khóa', value: false },
      ],
      onFilter: (value, record) => record.isActive === value,
    },
    {
      title: 'Ngày tham gia',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (text) => formatDateTime(text),
    },
    {
      title: 'Hành động',
      key: 'action',
      render: (_, record) => {
        // Prevent toggling current admin's state (optional guard, but users list usually drivers)
        if (record.role === 'ADMIN') return null;
        
        return (
          <Popconfirm
            title={record.isActive ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
            description={`Bạn có chắc chắn muốn ${record.isActive ? 'Khóa' : 'Kích hoạt'} tài xế này?`}
            onConfirm={() => handleToggleActive(record.id)}
            okText="Xác nhận"
            cancelText="Hủy"
            okButtonProps={{ danger: record.isActive }}
          >
            <Button
              type={record.isActive ? 'primary' : 'default'}
              danger={record.isActive}
              icon={<PoweroffOutlined />}
              size="small"
            >
              {record.isActive ? 'Khóa' : 'Kích hoạt'}
            </Button>
          </Popconfirm>
        );
      },
    },
  ];

  return (
    <Card className="glass-card" bordered={false}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {/* Search Filter bar */}
        <Row justify="space-between">
          <Col xs={24} sm={10}>
            <Input
              placeholder="Tìm kiếm tài xế theo tên, email, phone..."
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
              size="large"
            />
          </Col>
        </Row>

        {/* Users Table */}
        <Table
          dataSource={filteredUsers}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 8 }}
          size="middle"
        />
      </Space>
    </Card>
  );
};

export default UsersPage;
