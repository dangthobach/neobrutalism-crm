/**
 * ✅ PHASE 1 WEEK 3: Centralized Validation Schemas
 * 
 * Re-exports all Zod validation schemas for easy import
 */

// Customer validations
export {
  CustomerSchema,
  CreateCustomerSchema,
  UpdateCustomerSchema,
  CustomerTypeSchema,
  CustomerStatusSchema,
  type Customer,
  type CreateCustomer,
  type UpdateCustomer,
  type CustomerType,
  type CustomerStatus,
} from './customer'

// Contact validations
export {
  ContactSchema,
  CreateContactSchema,
  UpdateContactSchema,
  ContactTypeSchema,
  type Contact,
  type CreateContact,
  type UpdateContact,
  type ContactType,
} from './contact'

// Branch validations
export {
  BranchSchema,
  CreateBranchSchema,
  UpdateBranchSchema,
  BranchTypeSchema,
  BranchStatusSchema,
  type Branch,
  type CreateBranch,
  type UpdateBranch,
  type BranchType,
  type BranchStatus,
} from './branch'

// User validations
export {
  UserSchema,
  CreateUserSchema,
  UpdateUserSchema,
  ChangePasswordSchema,
  UserStatusSchema,
  type User,
  type CreateUser,
  type UpdateUser,
  type ChangePassword,
  type UserStatus,
} from './user'

// Role validations
export {
  RoleSchema,
  CreateRoleSchema,
  UpdateRoleSchema,
  RoleStatusSchema,
  RoleTypeSchema,
  PermissionSchema,
  SYSTEM_ROLES,
  DEFAULT_PERMISSIONS,
  type Role,
  type CreateRole,
  type UpdateRole,
  type RoleStatus,
  type RoleType,
  type Permission,
  type SystemRoleCode,
} from './role'

// Group validations
export {
  GroupSchema,
  CreateGroupSchema,
  UpdateGroupSchema,
  GroupTypeSchema,
  GroupStatusSchema,
  UserGroupSchema,
  AddUserToGroupSchema,
  RemoveUserFromGroupSchema,
  type Group,
  type CreateGroup,
  type UpdateGroup,
  type GroupType,
  type GroupStatus,
  type UserGroup,
  type AddUserToGroup,
  type RemoveUserFromGroup,
} from './group'
