import { create } from 'zustand';
import { slotApi } from '../api/slotApi';
import { message } from 'antd';

const useSlotStore = create((set, get) => ({
  slots: [],
  loading: false,
  error: null,

  fetchSlots: async () => {
    set({ loading: true, error: null });
    try {
      const { data } = await slotApi.getAllSlots();
      const slots = data.data || data;
      set({ slots: Array.isArray(slots) ? slots : [], loading: false });
    } catch (error) {
      set({ loading: false, error: error.message });
      message.error('Không thể tải dữ liệu bãi đỗ');
    }
  },

  updateSlot: (slotData) => {
    set((state) => {
      const slots = state.slots.map((slot) =>
        slot.id === slotData.id ? { ...slot, ...slotData } : slot
      );
      // If slot doesn't exist yet, add it
      if (!state.slots.find((s) => s.id === slotData.id)) {
        slots.push(slotData);
      }
      return { slots };
    });
  },

  updateMultipleSlots: (slotsData) => {
    set((state) => {
      const slotMap = new Map(state.slots.map((s) => [s.id, s]));
      slotsData.forEach((slotData) => {
        slotMap.set(slotData.id, { ...slotMap.get(slotData.id), ...slotData });
      });
      return { slots: Array.from(slotMap.values()) };
    });
  },
}));

export default useSlotStore;
