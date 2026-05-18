import { create } from "zustand";

type LocationState = {
  city: string;
  longitude?: number;
  latitude?: number;
  setLocation: (location: { city?: string; longitude?: number; latitude?: number }) => void;
};

export const useLocationStore = create<LocationState>((set) => ({
  city: "新港社区",
  setLocation: (location) =>
    set((state) => ({
      city: location.city ?? state.city,
      longitude: location.longitude,
      latitude: location.latitude
    }))
}));
