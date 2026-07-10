import React, { useEffect, useState } from 'react';
import {
  Card, Table, Button, Typography, Space, message, Modal, Form, Input, Select,
  Popconfirm, Tag, Empty, Spin,
} from 'antd';
import { PlusOutlined, DeleteOutlined, ReloadOutlined, CarOutlined } from '@ant-design/icons';
import { vehicleApi } from '../../api/vehicleApi';

const { Text } = Typography;

const vehicleTypes = ['CAR', 'MOTORBIKE', 'TRUCK', 'OTHER'];
const vehicleTypeLabels = {
  CAR: 'Ô tô',
  MOTORBIKE: 'Xe máy',
  TRUCK: 'Xe tải',
  OTHER: 'Khác',
};

const DriverVehiclesPage = () => {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [addModal, setAddModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [form] = Form.useForm();

  const fetchVehicles = async () => {
    setLoading(true);
    try {
      const res = await vehicleApi.getMyVehicles();
      const data = res.data?.data ?? res.data ?? [];
      setVehicles(Array.isArray(data) ? data : []);
    } catch (e) {
      message.error('Không thể tải danh sách xe');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVehicles();
  }, []);

  const handleAdd = async (values) => {
    setSubmitting(true);
    try {
      await vehicleApi.addVehicle(values);
      message.success('Thêm xe thành công!');
      setAddModal(false);
      form.resetFields();
      fetchVehicles();
    } catch (e) {
      message.error(e.response?.data?.message || 'Thêm xe thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    setDeletingId(id);
    try {
      await vehicleApi.deleteVehicle(id);
      message.success('Đã xóa xe thành công');
      fetchVehicles();
    } catch (e) {
      message.error(e.response?.data?.message || 'Xóa xe thất bại');
    } finally {
      setDeletingId(null);
    }
  };

  const columns = [
    {
      title: 'Biển số xe',
      dataIndex: 'licensePlate',
      key: 'licensePlate',
      render: (v) => (
        <Tag
          color="blue"
          style={{ fontWeight: 'bold', fontSize: 14, padding: '4px 12px' }}
        >
          {v}
        </Tag>
      ),
    },
    {
      title: 'Loại xe',
      dataIndex: 'vehicleType',
      key: 'vehicleType',
      render: (v) => vehicleTypeLabels[v] || v,
    },
    {
      title: 'Xe mặc định',
      dataIndex: 'isDefault',
      key: 'isDefault',
      render: (v) =>
        v ? <Tag color="green">Mặc định</Tag> : null,
    },
    {
      title: 'Hành động',
      key: 'action',
      render: (_, record) => (
        <Popconfirm
          title="Xóa xe?"
          description="Bạn có chắc muốn xóa xe này không?"
          onConfirm={() => handleDelete(record.id)}
          okText="Xóa"
          cancelText="Hủy"
          okButtonProps={{ danger: true }}
        >
          <Button
            danger
            size="small"
            icon={<DeleteOutlined />}
            loading={deletingId === record.id}
          >
            Xóa
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <Card
      title={
        <Space>
          <CarOutlined style={{ color: '#1677ff' }} />
          Xe của tôi
        </Space>
      }
      extra={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchVehicles} loading={loading} />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setAddModal(true)}
          >
            Thêm xe
          </Button>
        </Space>
      }
      style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}><Spin size="large" /></div>
      ) : vehicles.length === 0 ? (
        <Empty
          description="Bạn chưa đăng ký xe nào"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        >
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddModal(true)}>
            Thêm xe ngay
          </Button>
        </Empty>
      ) : (
        <Table
          dataSource={vehicles}
          columns={columns}
          rowKey="id"
          pagination={false}
        />
      )}

      <Modal
        title="Thêm xe mới"
        open={addModal}
        onCancel={() => { setAddModal(false); form.resetFields(); }}
        footer={null}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleAdd}>
          <Form.Item
            name="licensePlate"
            label="Biển số xe"
            rules={[{ required: true, message: 'Vui lòng nhập biển số xe!' }]}
          >
            <Input placeholder="Ví dụ: 30A-12345" style={{ textTransform: 'uppercase' }} />
          </Form.Item>

          <Form.Item
            name="vehicleType"
            label="Loại xe"
            rules={[{ required: true, message: 'Vui lòng chọn loại xe!' }]}
          >
            <Select placeholder="Chọn loại xe">
              {vehicleTypes.map((t) => (
                <Select.Option key={t} value={t}>
                  {vehicleTypeLabels[t]}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => { setAddModal(false); form.resetFields(); }}>
                Hủy
              </Button>
              <Button type="primary" htmlType="submit" loading={submitting} icon={<PlusOutlined />}>
                Thêm xe
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default DriverVehiclesPage;
