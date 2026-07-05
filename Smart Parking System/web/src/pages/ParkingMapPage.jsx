import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Segmented, Button, Tooltip, Modal, Descriptions, Tag, Space, Popconfirm, message } from 'antd';
import {
  CarOutlined,
  BorderOutlined,
  UnlockOutlined,
  LockOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import useSlotStore from '../stores/slotStore';
import { adminApi } from '../api/adminApi';
import { SLOT_STATUS } from '../constants';
import { subscribeToTopic, unsubscribeFromTopic } from '../utils/websocket';

const ParkingMapPage = () => {
  const { slots, loading, fetchSlots, updateSlot } = useSlotStore();
  const [selectedZone, setSelectedZone] = useState('A');
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [gateLoading, setGateLoading] = useState({ gate1: false, gate2: false });

  useEffect(() => {
    fetchSlots();

    // Subscribe to WebSocket for real-time slot state changes
    subscribeToTopic('/topic/slots', (updatedSlot) => {
      console.log('[ParkingMap] Received real-time slot update:', updatedSlot);
      updateSlot(updatedSlot);
    });

    return () => {
      unsubscribeFromTopic('/topic/slots');
    };
  }, []);

  const handleSlotClick = (slot) => {
    setSelectedSlot(slot);
    setModalVisible(true);
  };

  const handleGateControl = async (gateId, action) => {
    setGateLoading((prev) => ({ ...prev, [gateId]: true }));
    try {
      await adminApi.controlGate(gateId, action);
      message.success(`Đã gửi lệnh ${action === 'OPEN' ? 'MỞ' : 'ĐÓNG'} cổng ${gateId.toUpperCase()} thành công!`);
    } catch (error) {
      message.error(`Không thể gửi lệnh điều khiển cổng: ${error.message}`);
    } finally {
      setGateLoading((prev) => ({ ...prev, [gateId]: false }));
    }
  };

  const filteredSlots = slots.filter((slot) => slot.zone.toUpperCase() === selectedZone.toUpperCase());

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {/* Zone Selector and Map legends */}
      <Card className="glass-card" bordered={false}>
        <Row align="middle" justify="space-between" gutter={[16, 16]}>
          <Col>
            <Space size="middle">
              <span style={{ fontWeight: 'bold' }}>Chọn khu vực:</span>
              <Segmented
                options={[
                  { label: 'Khu A', value: 'A' },
                  { label: 'Khu B', value: 'B' },
                ]}
                value={selectedZone}
                onChange={(value) => setSelectedZone(value)}
                size="large"
              />
            </Space>
          </Col>
          <Col>
            <Space size="large" wrap>
              {Object.values(SLOT_STATUS).map((status) => (
                <Space key={status.key} size="small">
                  <div
                    style={{
                      width: 14,
                      height: 14,
                      borderRadius: '50%',
                      backgroundColor: status.color,
                    }}
                  />
                  <span>{status.label}</span>
                </Space>
              ))}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Grid Map */}
      <Card className="glass-card" title={`Sơ đồ bãi xe - Khu ${selectedZone}`} bordered={false} loading={loading}>
        <div className="parking-grid">
          {filteredSlots.map((slot) => {
            const statusConfig = SLOT_STATUS[slot.status] || SLOT_STATUS.AVAILABLE;
            return (
              <Tooltip
                key={slot.id}
                title={`Click để xem chi tiết slot ${slot.slotCode}`}
                placement="top"
              >
                <div
                  className="parking-slot-item"
                  onClick={() => handleSlotClick(slot)}
                  style={{
                    backgroundColor: statusConfig.bgColor,
                    color: statusConfig.color,
                  }}
                >
                  <CarOutlined style={{ fontSize: 26, marginBottom: 8 }} />
                  <span>{slot.slotCode}</span>
                  <span style={{ fontSize: 10, marginTop: 4, fontWeight: 'normal', opacity: 0.8 }}>
                    {statusConfig.label}
                  </span>
                </div>
              </Tooltip>
            );
          })}
        </div>
      </Card>

      {/* Barrier Gate Controllers */}
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card
            title={
              <Space>
                <BorderOutlined />
                <span>Barrier Cổng Vào (Gate 1)</span>
              </Space>
            }
            className="glass-card"
            bordered={false}
          >
            <div style={{ textAlign: 'center', padding: '16px 0' }}>
              <Space size="large">
                <Popconfirm
                  title="Mở cổng"
                  description="Bạn chắc chắn muốn gửi lệnh MỞ Barrier cổng VÀO?"
                  onConfirm={() => handleGateControl('gate1', 'OPEN')}
                  okText="Mở"
                  cancelText="Hủy"
                >
                  <Button
                    type="primary"
                    size="large"
                    icon={<UnlockOutlined />}
                    loading={gateLoading.gate1}
                    style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
                  >
                    Mở Barrier
                  </Button>
                </Popconfirm>
                <Popconfirm
                  title="Đóng cổng"
                  description="Bạn chắc chắn muốn gửi lệnh ĐÓNG Barrier cổng VÀO?"
                  onConfirm={() => handleGateControl('gate1', 'CLOSE')}
                  okText="Đóng"
                  cancelText="Hủy"
                >
                  <Button
                    type="primary"
                    size="large"
                    danger
                    icon={<LockOutlined />}
                    loading={gateLoading.gate1}
                  >
                    Đóng Barrier
                  </Button>
                </Popconfirm>
              </Space>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card
            title={
              <Space>
                <BorderOutlined />
                <span>Barrier Cổng Ra (Gate 2)</span>
              </Space>
            }
            className="glass-card"
            bordered={false}
          >
            <div style={{ textAlign: 'center', padding: '16px 0' }}>
              <Space size="large">
                <Popconfirm
                  title="Mở cổng"
                  description="Bạn chắc chắn muốn gửi lệnh MỞ Barrier cổng RA?"
                  onConfirm={() => handleGateControl('gate2', 'OPEN')}
                  okText="Mở"
                  cancelText="Hủy"
                >
                  <Button
                    type="primary"
                    size="large"
                    icon={<UnlockOutlined />}
                    loading={gateLoading.gate2}
                    style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
                  >
                    Mở Barrier
                  </Button>
                </Popconfirm>
                <Popconfirm
                  title="Đóng cổng"
                  description="Bạn chắc chắn muốn gửi lệnh ĐÓNG Barrier cổng RA?"
                  onConfirm={() => handleGateControl('gate2', 'CLOSE')}
                  okText="Đóng"
                  cancelText="Hủy"
                >
                  <Button
                    type="primary"
                    size="large"
                    danger
                    icon={<LockOutlined />}
                    loading={gateLoading.gate2}
                  >
                    Đóng Barrier
                  </Button>
                </Popconfirm>
              </Space>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Slot Details Modal */}
      <Modal
        title={
          <Space>
            <InfoCircleOutlined style={{ color: '#1a237e' }} />
            <span>Chi tiết ô đỗ {selectedSlot?.slotCode}</span>
          </Space>
        }
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setModalVisible(false)}>
            Đóng
          </Button>,
        ]}
      >
        {selectedSlot && (
          <Descriptions bordered column={1} size="small" style={{ marginTop: 16 }}>
            <Descriptions.Item label="Mã vị trí">
              <Tag color="blue" style={{ fontSize: 14, padding: '4px 8px' }}>
                {selectedSlot.slotCode}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Khu vực">{`Khu ${selectedSlot.zone}`}</Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color={SLOT_STATUS[selectedSlot.status]?.tag || 'default'}>
                {SLOT_STATUS[selectedSlot.status]?.label || selectedSlot.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Mã cảm biến (Sensor UID)">
              <code>{selectedSlot.sensorId || 'Chưa cấu hình'}</code>
            </Descriptions.Item>
            
            {/* Display mock details if occupied/reserved */}
            {selectedSlot.status === 'OCCUPIED' && (
              <>
                <Descriptions.Item label="Biển số xe đỗ">30A-987.65</Descriptions.Item>
                <Descriptions.Item label="Check-in lúc">12:35 (Hôm nay)</Descriptions.Item>
              </>
            )}
            {selectedSlot.status === 'RESERVED' && (
              <>
                <Descriptions.Item label="Người đặt trước">Nguyễn Văn A</Descriptions.Item>
                <Descriptions.Item label="Biển số xe">29K-123.45</Descriptions.Item>
                <Descriptions.Item label="Thời gian hẹn">19:30 - 21:30</Descriptions.Item>
              </>
            )}
          </Descriptions>
        )}
      </Modal>
    </Space>
  );
};

export default ParkingMapPage;
